import { Component, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { EventSourcePolyfill } from 'event-source-polyfill';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { CardModule } from 'primeng/card';
import { BadgeModule } from 'primeng/badge';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { SidebarModule } from 'primeng/sidebar';
import { DividerModule } from 'primeng/divider';

import { ChatbotService, ChatbotResponse, ChatbotIntent } from '../../services/chatbot.service';
import { ToastService } from '../../shared/services/toast.service';
import { BreadcrumbService } from '../../shared/services/breadcrumb.service';
import { DataRefreshService, RefreshType } from '../../shared/services/data-refresh.service';
import { environment } from '../../../environments/environment';

interface ChatMessage {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  timestamp: Date;
  intent?: ChatbotIntent;
  actions?: Array<{
    type: 'CREATE' | 'UPDATE' | 'DELETE' | 'NAVIGATE' | 'LIST';
    label: string;
    data?: unknown;
    route?: string[];
  }>;
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
    ToggleButtonModule,
    SidebarModule,
    DividerModule
  ],
  providers: [MessageService],
  templateUrl: './unified-chatbot.component.html',
  styleUrls: ['./unified-chatbot.component.css']
})
export class UnifiedChatbotComponent implements OnInit, OnDestroy {
  messages: ChatMessage[] = [];
  currentMessage = '';
  isLoading = false;
  isStreaming = false;
  isDarkMode = false;
  sidebarVisible = false;
  conversationHistory: ChatMessage[][] = [];
  currentConversationIndex = -1;

  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  // Configuration du chatbot
  chatbotConfig = {
    title: 'Assistant IA Assurance',
    subtitle: 'Votre assistant intelligent pour configurer des produits, packs et des garanties.',
  };

 private chatbotSubscription?: Subscription;
  private eventSource?: EventSourcePolyfill;

  constructor(
    private readonly chatbotService: ChatbotService,
    private readonly toastService: ToastService,
    private readonly breadcrumbService: BreadcrumbService,
    private readonly dataRefreshService: DataRefreshService,
    private readonly router: Router,
    private readonly messageService: MessageService
  ) {
    // Load dark mode preference
    this.isDarkMode = localStorage.getItem('darkMode') === 'true';
    document.body.classList.toggle('dark-mode', this.isDarkMode);
  }

  ngOnInit(): void {
    try {
      this.breadcrumbService.setChatbotBreadcrumb();

      // Load conversation history from localStorage
      this.loadConversationHistory();

      // Message de bienvenue
      const welcomeMessage = `${this.chatbotConfig.title}

${this.chatbotConfig.subtitle}

Comment puis-je vous aider aujourd'hui ?`;
      this.addBotMessage(welcomeMessage);
    } catch (error) {
      console.error('Erreur lors de l\'initialisation:', error);
      this.toastService.showWarning('Initialisation', 'Certaines fonctionnalités peuvent ne pas être disponibles');
    }
  }

  ngOnDestroy(): void {
    if (this.chatbotSubscription) {
      this.chatbotSubscription.unsubscribe();
    }
    if (this.eventSource) {
      this.eventSource.close();
    }
    // Save current conversation before leaving
    this.saveCurrentConversation();
  }

  sendMessage(): void {
    if (!this.currentMessage.trim() || this.isLoading || this.isStreaming) {
      return;
    }
   const userMessage = this.currentMessage.trim().toLowerCase();
    this.addUserMessage(this.currentMessage.trim());
    this.currentMessage = '';
    this.isLoading = true;

    // Detect if this is a business operation (create product/guarantie/pack)
    const isBusinessOperation = userMessage.includes('créer') ||
                                userMessage.includes('créer une') ||
                                userMessage.includes('créer un') ||
                                userMessage.includes('create') ||
                                userMessage.includes('ajouter') ||
                                userMessage.includes('nouveau') ||
                                userMessage.includes('configurer');

    if (isBusinessOperation) {
      // Use business process endpoint for operations
      this.sendMessageRegular(this.currentMessage.trim() || userMessage);
    } else {
      // Use streaming endpoint for conversational AI
      this.sendMessageWithStream(this.currentMessage.trim() || userMessage);
    }
  }

  private sendMessageWithStream(message: string): void {
    this.isStreaming = true;
    let fullResponse = '';
    let botMessageId = '';

    try {
      // Get JWT token from localStorage
      const token = localStorage.getItem('token');

      // Create a placeholder bot message for streaming
      botMessageId = this.generateId();
      this.messages.push({
        id: botMessageId,
        sender: 'bot',
        text: '',
        timestamp: new Date()
      });

      // Use fetch for POST request with proper headers via gateway
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream'
      };

      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      fetch(`${environment.apiProduit}/chatbot/stream/chat`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ message })
      }).then(async (response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body?.getReader();
        const decoder = new TextDecoder();

        if (!reader) {
          throw new Error('Response body is null');
        }

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          const chunk = decoder.decode(value);
          const lines = chunk.split('\n');

          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.substring(6);
              if (data && data !== '[DONE]' && !data.startsWith('Error:')) {
                fullResponse += data;
                const messageIndex = this.messages.findIndex(m => m.id === botMessageId);
                if (messageIndex !== -1) {
                  this.messages[messageIndex].text = fullResponse;
                  this.scrollToBottom();
                }
              }
            }
          }
        }

        this.eventSource?.close();
        this.isStreaming = false;
        this.isLoading = false;
        this.scrollToBottom();
      }).catch((error) => {
        console.error('Streaming error:', error);
        this.isStreaming = false;
        this.messages = this.messages.filter(m => m.id === botMessageId);

        // Show user-friendly error message
        const errorMessage = this.getErrorMessage(error);
        this.toastService.showInfo('Mode streaming', errorMessage);

        // Fallback to regular request
        this.sendMessageRegular(message);
      });

    } catch (error) {
      console.error('Stream setup error:', error);
      this.isStreaming = false;
      this.messages = this.messages.filter(m => m.id === botMessageId);

      // Fallback to regular request
      this.sendMessageRegular(message);
    }
  }

  private sendMessageRegular(message: string): void {
    this.chatbotService.processPrompt({ prompt: message }).subscribe({
      next: (response) => {
        this.handleChatbotResponse(response);
        this.isLoading = false;
      },
      error: (error) => {
        const errorMessage = this.getErrorMessage(error);
        this.addBotMessage(`Erreur de communication avec l\'assistant\n\n${errorMessage}`);
        this.toastService.showError('Erreur chatbot', errorMessage);
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

    this.addBotMessage(message, response.intent, response.actions);

    // Rafraîchir les listes après création réussie
    if (response.success && response.intent) {
      this.refreshDataBasedOnIntent(response.intent);
    }
  }

  private refreshDataBasedOnIntent(intent: ChatbotIntent): void {
    switch (intent) {
      case ChatbotIntent.CREATE_PRODUIT:
      case ChatbotIntent.UPDATE_PRODUIT:
      case ChatbotIntent.DELETE_PRODUIT:
      case ChatbotIntent.LIST_PRODUITS:
        this.dataRefreshService.refreshProduits();
        break;
      case ChatbotIntent.CREATE_GARANTIE:
      case ChatbotIntent.UPDATE_GARANTIE:
      case ChatbotIntent.DELETE_GARANTIE:
      case ChatbotIntent.LIST_GARANTIES:
        this.dataRefreshService.refreshGaranties();
        break;
      case ChatbotIntent.CREATE_PACK:
      case ChatbotIntent.CONFIGURE_PACK:
      case ChatbotIntent.CREATE_PACK_WITH_GARANTIES:
      case ChatbotIntent.UPDATE_PACK:
      case ChatbotIntent.DELETE_PACK:
      case ChatbotIntent.LIST_PACKS:
        this.dataRefreshService.refreshPacks();
        break;
      default:
        this.dataRefreshService.refreshAll();
    }
  }

  private formatDataForDisplay(data: ChatbotResponse['data']): string {
    if (!data) return '';

    let formatted = '\n\n---\n\n';

    if (data.produit) {
      const produit = data.produit as { nomProduit: string; description: string; typeProduit: string; statut: string };
      formatted += `Produit proposé :\n`;
      formatted += `• Nom : ${produit.nomProduit}\n`;
      formatted += `• Description : ${produit.description}\n`;
      formatted += `• Type : ${produit.typeProduit}\n`;
      formatted += `• Statut : ${produit.statut}\n`;
    }

    if (data.garantie) {
      const garantie = data.garantie as { nomGarantie: string; description: string; tauxRemboursement: number; statut: string };
      formatted += `Garantie proposée :\n`;
      formatted += `• Nom : ${garantie.nomGarantie}\n`;
      formatted += `• Description : ${garantie.description}\n`;
      formatted += `• Taux : ${(garantie.tauxRemboursement * 100).toFixed(0)}%\n`;
      formatted += `• Statut : ${garantie.statut}\n`;
    }

    if (data.pack) {
      const pack = data.pack as { nomPack: string; description: string; prixMensuel: number; niveauCouverture: string; statut: string };
      formatted += `Pack proposé :\n`;
      formatted += `• Nom : ${pack.nomPack}\n`;
      formatted += `• Description : ${pack.description}\n`;
      formatted += `• Prix : ${pack.prixMensuel}€/mois\n`;
      formatted += `• Niveau : ${pack.niveauCouverture}\n`;
      formatted += `• Statut : ${pack.statut}\n`;
    }

    if (data.packGarantie) {
      const pg = data.packGarantie as { packId: string; garantieId: string; tauxRemboursement: number; plafond: number };
      formatted += `Association pack–garantie :\n`;
      formatted += `• Pack : ${pg.packId}\n`;
      formatted += `• Garantie : ${pg.garantieId}\n`;
      formatted += `• Taux : ${(Number(pg.tauxRemboursement) * 100).toFixed(0)}%\n`;
      formatted += `• Plafond : ${pg.plafond}\n`;
    }

    if (data.recommendations) {
      formatted += `Recommandations :\n`;
      data.recommendations.forEach((rec: unknown, index: number) => {
        const recObj = rec as { title: string; type: string; description: string; price?: number; features?: string[] };
        formatted += `\n${index + 1}. ${recObj.title}\n`;
        formatted += `• Type : ${recObj.type}\n`;
        formatted += `• Description : ${recObj.description}\n`;
        if (recObj.price) formatted += `• Prix : ${recObj.price}€\n`;
        if (recObj.features) formatted += `• Fonctionnalités : ${recObj.features.join(', ')}\n`;
      });
    }

    if (data.topPacks) {
      formatted += `Packs recommandés :\n`;
      data.topPacks.forEach((pack, index) => {
        formatted += `\n${index + 1}. ${pack.nomPack} (score: ${pack.score})\n`;
        formatted += `• ID pack : ${pack.packId}\n`;
        formatted += `• Raisons : ${pack.reason}\n`;
      });
    }

    if (data.topProduits) {
      formatted += `Produits recommandés :\n`;
      data.topProduits.forEach((produit, index) => {
        formatted += `\n${index + 1}. ${produit.nomProduit} (score: ${produit.score})\n`;
        formatted += `• ID produit : ${produit.produitId}\n`;
        formatted += `• Raisons : ${produit.reason}\n`;
      });
    }

    if (data.criteria) {
      formatted += `Critères utilisés :\n`;
      Object.entries(data.criteria).forEach(([key, value]) => {
        formatted += `• ${key} : ${JSON.stringify(value)}\n`;
      });
    }

    if (data.generatedPrompt) {
      formatted += `\nPrompt Gemini généré :\n`;
      formatted += `${data.generatedPrompt}\n`;
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
        const errorMessage = this.getErrorMessage(error);
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
      `${this.chatbotConfig.title}\n\nConversation effacée. Vous pouvez poser une nouvelle question.`
    );
  }
  toggleDarkMode(): void {
    this.isDarkMode = !this.isDarkMode;
    document.body.classList.toggle('dark-mode', this.isDarkMode);
    localStorage.setItem('darkMode', this.isDarkMode.toString());
  }

  toggleSidebar(): void {
    this.sidebarVisible = !this.sidebarVisible;
  }

  loadConversation(index: number): void {
    if (index >= 0 && index < this.conversationHistory.length) {
      this.messages = [...this.conversationHistory[index]];
      this.currentConversationIndex = index;
      this.sidebarVisible = false;
      this.scrollToBottom();
    }
  }

  deleteConversation(index: number): void {
    this.conversationHistory.splice(index, 1);
    this.saveConversationHistory();
    if (this.currentConversationIndex === index) {
      this.currentConversationIndex = -1;
    }
  }

  startNewConversation(): void {
    this.saveCurrentConversation();
    this.messages = [];
    this.currentConversationIndex = -1;
    this.sidebarVisible = false;
  }

  private saveCurrentConversation(): void {
    if (this.messages.length > 1) {
      if (this.currentConversationIndex >= 0) {
        this.conversationHistory[this.currentConversationIndex] = [...this.messages];
      } else {
        this.conversationHistory.unshift([...this.messages]);
        this.currentConversationIndex = 0;
      }
      this.saveConversationHistory();
    }
  }

  private saveConversationHistory(): void {
    try {
      localStorage.setItem('chatbotHistory', JSON.stringify(this.conversationHistory));
    } catch (error) {
      console.error('Erreur lors de la sauvegarde de l\'historique:', error);
      // Quota exceeded or storage disabled - silently fail
    }
  }

  private loadConversationHistory(): void {
    try {
      const saved = localStorage.getItem('chatbotHistory');
      if (saved) {
        this.conversationHistory = JSON.parse(saved);
      }
    } catch (error) {
      console.error('Erreur lors du chargement de l\'historique:', error);
      this.conversationHistory = [];
    }
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }

  getConversationPreview(conversation: ChatMessage[]): string {
    const userMessages = conversation.filter(m => m.sender === 'user');
    if (userMessages.length > 0) {
      return userMessages[0].text.substring(0, 50) + (userMessages[0].text.length > 50 ? '...' : '');
    }
    return 'Nouvelle conversation';
  }

  getConversationTime(conversation: ChatMessage[]): string {
    if (conversation.length > 0) {
      return this.formatTime(conversation[0].timestamp);
    }
    return '';
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

  private addBotMessage(text: string, intent?: ChatbotIntent, actions?: Array<{ type: 'CREATE' | 'UPDATE' | 'DELETE' | 'NAVIGATE' | 'LIST'; label: string; data?: unknown; route?: string[] }>): void {
    this.messages.push({
      id: this.generateId(),
      sender: 'bot',
      text,
      timestamp: new Date(),
      intent,
      actions
    });
  }

  private generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).substring(2);
  }

  private getErrorMessage(error: any): string {
    if (!error) return 'Erreur inconnue';

    // Check for HTTP errors
    if (error.status) {
      switch (error.status) {
        case 0:
          return 'Impossible de joindre le serveur. Vérifiez votre connexion.';
        case 400:
          return 'Requête invalide. Veuillez vérifier votre message.';
        case 401:
          return 'Non autorisé. Veuillez vous reconnecter.';
        case 403:
          return 'Accès refusé.';
        case 404:
          return 'Service non trouvé.';
        case 500:
          return 'Erreur interne du serveur. Veuillez réessayer plus tard.';
        case 503:
          return 'Service temporairement indisponible.';
        default:
          return `Erreur HTTP ${error.status}: ${error.statusText || 'Erreur inconnue'}`;
      }
    }

    // Check for network errors
    if (error.name === 'TypeError' && error.message?.includes('fetch')) {
      return 'Erreur réseau. Vérifiez votre connexion internet.';
    }

    // Check for timeout
    if (error.name === 'TimeoutError') {
      return 'Délai d\'attente dépassé. Le serveur met trop de temps à répondre.';
    }

    // Return custom error message or fallback
    return error.message || error.error?.message || 'Erreur inconnue';
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit'
    });
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
      [ChatbotIntent.RECOMMENDATION]: 'bi-lightbulb',
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
      [ChatbotIntent.RECOMMENDATION]: 'Recommandation',
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
      case ChatbotIntent.RECOMMENDATION:
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
      'RECOMMENDATION': 'Recommander un produit',
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
}
