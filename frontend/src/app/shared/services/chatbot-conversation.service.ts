import { Injectable } from '@angular/core';

export interface ChatMessage {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  timestamp: Date;
  intent?: any;
  actions?: Array<{
    type: 'CREATE' | 'UPDATE' | 'DELETE' | 'NAVIGATE' | 'LIST';
    label: string;
    data?: unknown;
    route?: string[];
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class ChatbotConversationService {
  private readonly STORAGE_KEY = 'chatbotHistory';

  saveCurrentConversation(messages: ChatMessage[], currentIndex: number): ChatMessage[][] {
    let history = this.loadConversationHistory();

    if (messages.length > 1) {
      if (currentIndex >= 0) {
        history[currentIndex] = [...messages];
      } else {
        history.unshift([...messages]);
        currentIndex = 0;
      }
    }

    this.saveConversationHistory(history);
    return history;
  }

  loadConversationHistory(): ChatMessage[][] {
    try {
      const saved = localStorage.getItem(this.STORAGE_KEY);
      if (saved) {
        return JSON.parse(saved);
      }
    } catch (error) {
      console.error('Erreur lors du chargement de l\'historique:', error);
    }
    return [];
  }

  saveConversationHistory(history: ChatMessage[][]): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(history));
    } catch (error) {
      console.error('Erreur lors de la sauvegarde de l\'historique:', error);
    }
  }

  loadConversation(index: number, history: ChatMessage[][]): ChatMessage[] | null {
    if (index >= 0 && index < history.length) {
      return [...history[index]];
    }
    return null;
  }

  deleteConversation(index: number, history: ChatMessage[][]): ChatMessage[][] {
    const newHistory = [...history];
    newHistory.splice(index, 1);
    this.saveConversationHistory(newHistory);
    return newHistory;
  }

  startNewConversation(): ChatMessage[] {
    return [];
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

  formatTime(date: Date): string {
    return date.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).substring(2);
  }
}
