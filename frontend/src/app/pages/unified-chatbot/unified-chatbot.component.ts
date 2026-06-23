import { Component, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { CardModule } from 'primeng/card';
import { BadgeModule } from 'primeng/badge';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';

import { ChatbotService, ChatbotResponse, ChatbotIntent } from '../../services/chatbot.service';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { KeyboardShortcutDirective, KeyboardShortcut } from '../../shared/directives/keyboard-shortcut.directive';
import { ClickOutsideDirective } from '../../shared/directives/click-outside.directive';
import { ConversationStorageService, Conversation } from '../../services/conversation-storage.service';
import { ExportService } from '../../services/export.service';

interface RecommendationItem {
  id: string;
  nom: string;
  description?: string;
  compatibilityScore: number;
  monthlyPrice?: number;
  coverageLevel?: string;
  whyRecommended?: string;
  explanation?: string;
}

interface ChatMessage {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  timestamp: Date;
  intent?: ChatbotIntent;
  data?: ChatbotResponse['data'];
  actions?: Array<{
    type: 'CREATE' | 'UPDATE' | 'DELETE' | 'NAVIGATE' | 'LIST';
    label: string;
    data?: unknown;
    route?: string[];
  }>;
}

interface SearchResult {
  message: ChatMessage;
  text: string;
  timestamp: Date;
}

@Component({
  selector: 'app-unified-chatbot',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ToastModule,
    ButtonModule,
    InputTextareaModule,
    ProgressSpinnerModule,
    CardModule,
    BadgeModule,
    TagModule,
    TooltipModule,
    KeyboardShortcutDirective,
    ClickOutsideDirective
  ],
  providers: [MessageService],
  templateUrl: './unified-chatbot.component.html',
  styleUrls: ['./unified-chatbot.component.css']
})
export class UnifiedChatbotComponent implements OnInit, OnDestroy {
  messages: ChatMessage[] = [];
  currentMessage = '';
  isLoading = false;

  // New functionality properties
  showHistory = false;
  showSearch = false;
  isListening = false;
  historySearchQuery = '';
  searchQuery = '';
  searchResults: SearchResult[] = [];
  conversations: Conversation[] = [];
  currentConversationId = '';
  filteredHistory: Conversation[] = [];

  @ViewChild('messagesContainer', { read: ElementRef }) messagesContainer!: ElementRef;

  // Voice recognition
  private recognition: any = null;

  // Configuration du chatbot
  chatbotConfig = {
    title: 'Assistant IA Assurance',
    subtitle: 'Votre assistant intelligent pour configurer des produits, garanties, packs.',
    features: [
      'Création de garanties',
      'Gestion des packs',
      'Configuration des produits',
      'Recommandations intelligentes'
    ],
    intents: [
      { icon: 'bi-box', label: 'CREATE_PRODUIT', description: 'Créer des produits d\'assurance' },
      { icon: 'bi-shield-check', label: 'CREATE_GARANTIE', description: 'Créer des garanties' },
      { icon: 'bi-collection', label: 'CREATE_PACK', description: 'Créer des packs' },
      { icon: 'bi-gear', label: 'CONFIGURE_PACK', description: 'Configurer des packs avec garanties' },
      { icon: 'bi-lightbulb', label: 'RECOMMENDATION', description: 'Obtenir des recommandations' }
    ]
  };

  readonly suggestionPrompts = [
    'Créer une garantie hospitalisation avec 90% de remboursement',
    'Créer un produit d\'assurance santé nommé Produit Santé Plus',
    'Créer un pack Gold lié au produit Santé Premium'
  ];

  private chatbotSubscription?: Subscription;

  constructor(
    private readonly chatbotService: ChatbotService,
    private readonly toastService: ToastService,
    private readonly breadcrumbService: BreadcrumbService,
    private readonly router: Router,
    private readonly messageService: MessageService,
    private readonly conversationStorage: ConversationStorageService,
    private readonly exportService: ExportService
  ) {}

  ngOnInit(): void {
    try {
      this.breadcrumbService.setChatbotBreadcrumb();

      // Load conversations from localStorage
      this.loadConversations();

      // Initialize voice recognition
      this.initVoiceRecognition();

      // Create initial conversation
      this.createConversation();

      // Message de bienvenue
      const welcomeMessage = `**${this.chatbotConfig.title}**

${this.chatbotConfig.subtitle}

Fonctionnalités disponibles :
${this.chatbotConfig.features.map(f => `• ${f}`).join('\n')}

Comment puis-je vous aider aujourd'hui ?`;
      
      this.addBotMessage(welcomeMessage);
    } catch (error) {
      console.error('ChatbotComponent: Error in ngOnInit', error);
    }
  }

  ngOnDestroy(): void {
    if (this.chatbotSubscription) {
      this.chatbotSubscription.unsubscribe();
    }
    if (this.recognition) {
      this.recognition.stop();
    }
    // Save current conversation before destroying
    this.saveCurrentConversation();
  }

  sendMessage(): void {
    if (!this.currentMessage.trim() || this.isLoading) {
      return;
    }

    const userMessage = this.currentMessage.trim();
    this.addUserMessage(userMessage);
    this.currentMessage = '';
    this.isLoading = true;

    this.chatbotService.processPrompt({ prompt: userMessage }).subscribe({
      next: (response) => {
        this.handleChatbotResponse(response);
        this.isLoading = false;
      },
      error: () => {
        this.addBotMessage('Erreur de communication avec l\'assistant. Veuillez réessayer.');
        this.toastService.showError('Erreur chatbot', 'Communication impossible');
        this.isLoading = false;
      }
    });
  }

  private handleChatbotResponse(response: ChatbotResponse): void {
    let message = response.message;

    // Ajouter les informations de validation si présentes
    if (response.validation) {
      if (response.validation.errors.length > 0) {
        message += '\n\n Erreurs : \n' + response.validation.errors.map(e => `• ${e}`).join('\n');
      }
      if (response.validation.warnings.length > 0) {
        message += '\n\n Avertissements :\n' + response.validation.warnings.map(w => `• ${w}`).join('\n');
      }
    }

    // Ajouter les données structurées si présentes
    if (response.data) {
      message += this.formatDataForDisplay(response.data);
    }

    this.addBotMessage(message, response.intent, response.actions, response.data);
  }

  private formatDataForDisplay(data: ChatbotResponse['data']): string {
    if (!data) return '';

    let formatted = '\n\n---\n';

    if (data.produit) {
      const produit = data.produit as { nomProduit: string; description: string; typeProduit: string; statut: string };
      formatted += `\n📦 **Produit**: ${produit.nomProduit}`;
      formatted += `\n   Type: ${produit.typeProduit} | Statut: ${produit.statut}`;
      if (produit.description && produit.description.length < 100) {
        formatted += `\n   ${produit.description}`;
      }
    }

    if (data.garantie) {
      const garantie = data.garantie as { nomGarantie: string; description: string; domaine?: string; tauxRemboursement: number; statut: string; dureeMinContrat?: number; dureeMaxContrat?: number; typeMontant?: string };
      const cleanedName = this.cleanGarantieName(garantie.nomGarantie);
      formatted += `\n🛡️ **Garantie**: ${cleanedName}`;
      formatted += `\n   Taux: ${(garantie.tauxRemboursement * 100).toFixed(0)}% | Domaine: ${garantie.domaine ?? '—'} | Statut: ${garantie.statut}`;
      if (garantie.dureeMinContrat || garantie.dureeMaxContrat) {
        formatted += `\n   Durée: ${garantie.dureeMinContrat || 0} – ${garantie.dureeMaxContrat || 0} mois`;
      }
      if (garantie.description && garantie.description.length < 80) {
        formatted += `\n   ${garantie.description}`;
      }
    }

    if (data.pack) {
      const pack = data.pack as { nomPack: string; description: string; prixMensuel: number; niveauCouverture: string; statut: string };
      formatted += `\n📋 **Pack**: ${pack.nomPack}`;
      formatted += `\n   Prix: ${pack.prixMensuel}€/mois | Niveau: ${pack.niveauCouverture} | Statut: ${pack.statut}`;
      if (pack.description && pack.description.length < 80) {
        formatted += `\n   ${pack.description}`;
      }
    }

    if (data.packGarantie) {
      const pg = data.packGarantie as { packId: string; garantieId: string; tauxRemboursement: number; plafond: number };
      formatted += `\n🔗 **Association**: Pack → Garantie`;
      formatted += `\n   Taux: ${(Number(pg.tauxRemboursement) * 100).toFixed(0)}% | Plafond: ${pg.plafond}€`;
    }

    if (data.recommendations) {
      formatted += `\n🎯 **Recommandations**:\n`;
      data.recommendations.slice(0, 3).forEach((rec: unknown, index: number) => {
        const recObj = rec as { id: string; nom: string; description: string; compatibilityScore: number; monthlyPrice?: number; coverageLevel?: string; whyRecommended?: string; explanation?: string };
        formatted += `\n${index + 1}. **${recObj.nom}** (${recObj.compatibilityScore.toFixed(1)}%)`;
        if (recObj.monthlyPrice) formatted += ` - ${recObj.monthlyPrice}€/mois`;
        if (recObj.whyRecommended && recObj.whyRecommended.length < 100) {
          formatted += `\n   ✅ ${recObj.whyRecommended}`;
        }
      });
    }

    return formatted;
  }

  executeAction(action: { type: 'CREATE' | 'UPDATE' | 'DELETE' | 'NAVIGATE' | 'LIST'; label: string; data?: unknown; route?: string[] }): void {
    switch (action.type) {
      case 'CREATE':
        this.executeCreateAction(action as { type: 'CREATE'; label: string; data?: unknown });
        break;
      case 'NAVIGATE':
        if (action.route) {
          this.router.navigate(action.route);
        }
        break;
      default:
        this.toastService.showInfo('Action non implémentée', `Type : ${action.type}`);
    }
  }

  private executeCreateAction(action: { type: 'CREATE'; label: string; data?: unknown }): void {
    this.isLoading = true;

    // Simuler la création via le chatbot
    this.chatbotService.processPrompt({ prompt: `Créer ${action.label}` }).subscribe({
      next: (response) => {
        this.addBotMessage(response.message);
        if (response.success) {
          this.toastService.showSuccess(`${action.label} créé avec succès`);
        } else {
          this.toastService.showError('Échec création', response.error || 'Erreur inconnue');
        }
        this.isLoading = false;
      },
      error: (error) => {
        const errorMessage = error?.message || 'Erreur de communication';
        this.addBotMessage(`Erreur de communication\n\n${errorMessage}`);
        this.toastService.showError('Erreur communication', errorMessage);
        this.isLoading = false;
      }
    });
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    } else if (event.key === 'Enter' && event.shiftKey) {
      // Allow newline with Shift+Enter
      const target = event.target as HTMLTextAreaElement;
      const start = target.selectionStart;
      const end = target.selectionEnd;
      const value = target.value;

      target.value = value.substring(0, start) + '\n' + value.substring(end);
      target.selectionStart = target.selectionEnd = start + 1;

      event.preventDefault();
    }
  }

  clearChat(): void {
    this.messages = [];
    this.addBotMessage(
      `**${this.chatbotConfig.title}**\n\nConversation effacée. Vous pouvez poser une nouvelle question.`
    );
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  applySuggestion(text: string): void {
    if (this.isLoading) {
      return;
    }
    this.currentMessage = text;
    this.sendMessage();
  }

  private addUserMessage(text: string): void {
    this.messages.push({
      id: this.generateId(),
      sender: 'user',
      text,
      timestamp: new Date()
    });
  }

  private addBotMessage(text: string, intent?: ChatbotIntent, actions?: Array<{ type: 'CREATE' | 'UPDATE' | 'DELETE' | 'NAVIGATE' | 'LIST'; label: string; data?: unknown; route?: string[] }>, data?: ChatbotResponse['data']): void {
    this.messages.push({
      id: this.generateId(),
      sender: 'bot',
      text,
      timestamp: new Date(),
      intent,
      actions,
      data
    });
  }

  private generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getRecommendations(message: ChatMessage): RecommendationItem[] {
    if (!message.data?.recommendations) return [];
    return message.data.recommendations as unknown as RecommendationItem[];
  }

  getExplanation(message: ChatMessage): string {
    if (!message.data?.explanation) return '';
    return message.data.explanation;
  }

  getIntentIcon(intent?: ChatbotIntent): string {
    const icons: Record<ChatbotIntent, string> = {
      [ChatbotIntent.CREATE_PRODUIT]: 'bi-box',
      [ChatbotIntent.CREATE_GARANTIE]: 'bi-shield-check',
      [ChatbotIntent.CREATE_PACK]: 'bi-collection',
      [ChatbotIntent.CONFIGURE_PACK]: 'bi-gear',
      [ChatbotIntent.CREATE_PACK_WITH_GARANTIES]: 'bi-layers',
      [ChatbotIntent.UPDATE_PRODUIT]: 'bi-pencil',
      [ChatbotIntent.UPDATE_GARANTIE]: 'bi-pencil',
      [ChatbotIntent.UPDATE_PACK]: 'bi-pencil',
      [ChatbotIntent.DELETE_PRODUIT]: 'bi-trash',
      [ChatbotIntent.DELETE_GARANTIE]: 'bi-trash',
      [ChatbotIntent.DELETE_PACK]: 'bi-trash',
      [ChatbotIntent.LIST_PRODUITS]: 'bi-list',
      [ChatbotIntent.LIST_GARANTIES]: 'bi-list',
      [ChatbotIntent.LIST_PACKS]: 'bi-list',
      [ChatbotIntent.RECOMMANDATION]: 'bi-lightbulb',
      [ChatbotIntent.HELP]: 'bi-question-circle',
      [ChatbotIntent.UNKNOWN]: 'bi-question'
    };
    return icons[intent || ChatbotIntent.UNKNOWN] || 'bi-question';
  }

  getIntentLabel(intent?: ChatbotIntent): string {
    if (!intent) {
      return 'Assistant';
    }
    const labels: Record<ChatbotIntent, string> = {
      [ChatbotIntent.CREATE_PRODUIT]: 'Création produit',
      [ChatbotIntent.CREATE_GARANTIE]: 'Création garantie',
      [ChatbotIntent.CREATE_PACK]: 'Création pack',
      [ChatbotIntent.CONFIGURE_PACK]: 'Configuration pack',
      [ChatbotIntent.CREATE_PACK_WITH_GARANTIES]: 'Pack + garanties',
      [ChatbotIntent.UPDATE_PRODUIT]: 'Mise à jour produit',
      [ChatbotIntent.UPDATE_GARANTIE]: 'Mise à jour garantie',
      [ChatbotIntent.UPDATE_PACK]: 'Mise à jour pack',
      [ChatbotIntent.DELETE_PRODUIT]: 'Suppression produit',
      [ChatbotIntent.DELETE_GARANTIE]: 'Suppression garantie',
      [ChatbotIntent.DELETE_PACK]: 'Suppression pack',
      [ChatbotIntent.LIST_PRODUITS]: 'Liste produits',
      [ChatbotIntent.LIST_GARANTIES]: 'Liste garanties',
      [ChatbotIntent.LIST_PACKS]: 'Liste packs',
      [ChatbotIntent.RECOMMANDATION]: 'Recommandation',
      [ChatbotIntent.HELP]: 'Aide',
      [ChatbotIntent.UNKNOWN]: 'Général'
    };
    return labels[intent] || intent.replace(/_/g, ' ');
  }

  getIntentSeverity(intent?: ChatbotIntent): 'success' | 'info' | 'warning' | 'danger' | 'secondary' | 'contrast' {
    switch (intent) {
      case ChatbotIntent.CREATE_GARANTIE:
      case ChatbotIntent.CREATE_PRODUIT:
      case ChatbotIntent.CREATE_PACK:
        return 'success';
      case ChatbotIntent.CONFIGURE_PACK:
      case ChatbotIntent.CREATE_PACK_WITH_GARANTIES:
        return 'info';
      case ChatbotIntent.UPDATE_PRODUIT:
      case ChatbotIntent.UPDATE_GARANTIE:
      case ChatbotIntent.UPDATE_PACK:
        return 'warning';
      case ChatbotIntent.DELETE_PRODUIT:
      case ChatbotIntent.DELETE_GARANTIE:
      case ChatbotIntent.DELETE_PACK:
        return 'danger';
      case ChatbotIntent.RECOMMANDATION:
        return 'contrast';
      default:
        return 'secondary';
    }
  }

  formatMessage(text: string): string {
    return text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
               .replace(/\n/g, '<br>');
  }

  trackByMessageId(index: number, message: ChatMessage): string {
    return message.id;
  }

  trackByActionLabel(index: number, action: any): string {
    return action.label;
  }

  trackByIntentLabel(index: number, intent: any): string {
    return intent.label;
  }

  // History functionality
  toggleHistory(): void {
    this.showHistory = !this.showHistory;
    if (this.showHistory) {
      this.filteredHistory = [...this.conversations];
    }
  }

  loadConversations(): void {
    this.conversationStorage.getAllConversations().subscribe({
      next: (conversations) => {
        this.conversations = conversations;
        this.filteredHistory = [...conversations];
      },
      error: (error) => {
        console.error('Error loading conversations', error);
        this.conversations = [];
      }
    });
  }

  createConversation(): void {
    const newConversation: Conversation = {
      id: this.generateId(),
      title: 'Nouvelle conversation',
      messages: [],
      createdAt: new Date(),
      updatedAt: new Date()
    };
    this.conversations.unshift(newConversation);
    this.currentConversationId = newConversation.id;
    this.conversationStorage.saveConversation(newConversation).subscribe();
  }

  saveCurrentConversation(): void {
    const conversation = this.conversations.find(c => c.id === this.currentConversationId);
    if (conversation) {
      conversation.messages = [...this.messages];
      conversation.updatedAt = new Date();

      // Update title based on first user message
      if (conversation.messages.length > 0) {
        const firstUserMsg = conversation.messages.find(m => m.sender === 'user');
        if (firstUserMsg && conversation.title === 'Nouvelle conversation') {
          conversation.title = firstUserMsg.text.substring(0, 50) + (firstUserMsg.text.length > 50 ? '...' : '');
        }
      }

      this.conversationStorage.saveConversation(conversation).subscribe();
    }
  }

  loadConversation(conversationId: string): void {
    const conversation = this.conversations.find(c => c.id === conversationId);
    if (conversation) {
      this.currentConversationId = conversationId;
      this.messages = conversation.messages as ChatMessage[];
      this.showHistory = false;
      this.scrollToBottom();
      this.toastService.showInfo('Conversation chargée', conversation.title);
    }
  }

  deleteConversation(conversationId: string, event: Event): void {
    event.stopPropagation();
    this.conversationStorage.deleteConversation(conversationId).subscribe({
      next: () => {
        this.conversations = this.conversations.filter(c => c.id !== conversationId);
        this.filteredHistory = this.filteredHistory.filter(c => c.id !== conversationId);

        if (this.currentConversationId === conversationId) {
          this.clearChat();
          this.createConversation();
        }

        this.toastService.showSuccess('Conversation supprimée');
      },
      error: () => {
        this.toastService.showError('Erreur', 'Impossible de supprimer la conversation');
      }
    });
  }

  searchHistory(): void {
    if (!this.historySearchQuery.trim()) {
      this.filteredHistory = [...this.conversations];
      return;
    }

    this.conversationStorage.searchConversations(this.historySearchQuery).subscribe({
      next: (results) => {
        this.filteredHistory = results;
      },
      error: () => {
        this.filteredHistory = [];
      }
    });
  }

  // Search functionality
  toggleSearch(): void {
    this.showSearch = !this.showSearch;
    if (this.showSearch) {
      setTimeout(() => {
        const searchInput = document.querySelector('.search-input') as HTMLInputElement;
        if (searchInput) {
          searchInput.focus();
        }
      }, 100);
    }
  }

  searchMessages(): void {
    if (!this.searchQuery.trim()) {
      this.searchResults = [];
      return;
    }

    this.searchResults = [];

    this.messages.forEach(msg => {
      if (msg.text.toLowerCase().includes(this.searchQuery.toLowerCase())) {
        this.searchResults.push({
          message: msg,
          text: msg.text,
          timestamp: msg.timestamp
        });
      }
    });
  }

  goToMessage(result: SearchResult): void {
    const messageElement = document.querySelector(`[data-message-id="${result.message.id}"]`);
    if (messageElement) {
      messageElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
      messageElement.classList.add('highlighted');
      setTimeout(() => {
        messageElement.classList.remove('highlighted');
      }, 2000);
    }
    this.showSearch = false;
    this.searchQuery = '';
    this.searchResults = [];
  }

  highlightSearchTerms(text: string, query: string): string {
    if (!query.trim()) return text;
    const regex = new RegExp(`(${query})`, 'gi');
    return text.replace(regex, '<mark>$1</mark>');
  }

  // Export functionality
  exportConversation(): void {
    const conversation = this.conversations.find(c => c.id === this.currentConversationId);
    if (!conversation) {
      this.toastService.showError('Erreur', 'Aucune conversation à exporter');
      return;
    }

    const title = conversation.title || 'Conversation';
    
    this.exportService.exportToTXT([conversation]).subscribe({
      next: (content) => {
        this.exportService.downloadFile(content, `${title.replace(/[^a-z0-9]/gi, '_')}_${new Date().toISOString().split('T')[0]}.txt`, 'text/plain');
        this.toastService.showSuccess('Export réussi', 'Conversation téléchargée');
      },
      error: () => {
        this.toastService.showError('Erreur', 'Impossible d\'exporter la conversation');
      }
    });
  }

  // Voice input functionality
  initVoiceRecognition(): void {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      this.recognition = new SpeechRecognition();
      this.recognition.continuous = false;
      this.recognition.interimResults = false;
      this.recognition.lang = 'fr-FR';

      this.recognition.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        this.currentMessage = transcript;
        this.isListening = false;
        this.toastService.showSuccess('Dictée terminée', 'Message transcrit');
      };

      this.recognition.onerror = (event: any) => {
        console.error('Speech recognition error:', event.error);
        this.isListening = false;
        this.toastService.showError('Erreur de dictée', 'Veuillez réessayer');
      };

      this.recognition.onend = () => {
        this.isListening = false;
      };
    }
  }

  toggleVoiceInput(): void {
    if (!this.recognition) {
      this.toastService.showError('Dictée non disponible', 'Votre navigateur ne supporte pas la reconnaissance vocale');
      return;
    }

    if (this.isListening) {
      this.recognition.stop();
      this.isListening = false;
    } else {
      this.recognition.start();
      this.isListening = true;
      this.toastService.showInfo('Dictée en cours', 'Parlez maintenant...');
    }
  }

  // Keyboard shortcuts
  handleKeyboardShortcut(shortcut: KeyboardShortcut): void {
    if (shortcut.ctrl && shortcut.key === 'k') {
      this.toggleSearch();
    } else if (shortcut.ctrl && shortcut.key === 'e') {
      this.exportConversation();
    } else if (shortcut.ctrl && shortcut.key === 'n') {
      this.clearChat();
      this.createConversation();
    } else if (shortcut.key === 'Escape') {
      if (this.showSearch) {
        this.showSearch = false;
      }
      if (this.showHistory) {
        this.showHistory = false;
      }
      if (this.isListening) {
        this.toggleVoiceInput();
      }
    } else if (shortcut.key === 'Enter') {
      this.sendMessage();
    }
  }

  // Helper methods
  scrollToBottom(): void {
    setTimeout(() => {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }

  trackByConversationId(index: number, conv: Conversation): string {
    return conv.id;
  }

  trackBySearchResult(index: number, result: SearchResult): string {
    return result.message.id;
  }

  formatDateRelative(date: Date): string {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'À l\'instant';
    if (diffMins < 60) return `Il y a ${diffMins} min`;
    if (diffHours < 24) return `Il y a ${diffHours} h`;
    if (diffDays < 7) return `Il y a ${diffDays} j`;
    return date.toLocaleDateString('fr-FR');
  }

  getIntentExample(intentLabel: string): string {
    const examples = {
      'CREATE_PRODUIT': 'Créer un nouveau produit d\'assurance',
      'CREATE_GARANTIE': 'Créer une garantie hospitalisation',
      'CREATE_PACK': 'Créer un pack santé',
      'CONFIGURE_PACK': 'Configurer un pack avec garanties',
      'CREATE_PACK_WITH_GARANTIES': 'Créer un pack complet',
      'UPDATE_PRODUIT': 'Modifier un produit existant',
      'UPDATE_GARANTIE': 'Mettre à jour une garantie',
      'UPDATE_PACK': 'Modifier un pack',
      'DELETE_PRODUIT': 'Supprimer un produit',
      'DELETE_GARANTIE': 'Supprimer une garantie',
      'DELETE_PACK': 'Supprimer un pack',
      'LIST_PRODUITS': 'Lister tous les produits',
      'LIST_GARANTIES': 'Voir les garanties',
      'LIST_PACKS': 'Afficher les packs',
      'RECOMMANDATION': 'Recommander un produit',
      'HELP': 'Aide',
      'UNKNOWN': 'Aide'
    };
    return examples[intentLabel as keyof typeof examples] || 'Aide';
  }

  getActionIcon(actionType: 'CREATE' | 'UPDATE' | 'DELETE' | 'NAVIGATE' | 'LIST' | string): string {
    const icons: Record<string, string> = {
      'CREATE': 'bi-plus-circle',
      'UPDATE': 'bi-pencil',
      'DELETE': 'bi-trash',
      'NAVIGATE': 'bi-arrow-right',
      'LIST': 'bi-list'
    };
    return icons[actionType] || 'bi-circle';
  }

  private cleanGarantieName(nom: string): string {
    if (!nom) return nom;
    // Supprimer "nommee " au début du nom
    if (nom.toLowerCase().startsWith('nommee ')) {
      return nom.substring(7).trim();
    }
    return nom;
  }
}
