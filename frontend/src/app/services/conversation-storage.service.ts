import { Injectable } from '@angular/core';
import localforage from 'localforage';
import { Observable, from, of, throwError, forkJoin } from 'rxjs';
import { map, catchError, switchMap } from 'rxjs/operators';

export interface Conversation {
  id: string;
  title: string;
  messages: ChatMessage[];
  createdAt: Date;
  updatedAt: Date;
  sessionId?: string;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  timestamp: Date;
  intent?: string;
  data?: any;
}

@Injectable({
  providedIn: 'root'
})
export class ConversationStorageService {
  private readonly storageKey = 'chatbot_conversations';
  private readonly currentSessionKey = 'chatbot_current_session';
  private readonly maxConversations = 50;
  private db: LocalForage;

  constructor() {
    this.db = localforage.createInstance({
      name: 'VermegChatbot',
      storeName: 'conversations'
    });
  }

  /**
   * Save a conversation to storage
   */
  saveConversation(conversation: Conversation): Observable<Conversation> {
    return from(this.db.setItem(conversation.id, conversation)).pipe(
      map(() => conversation),
      catchError(error => {
        console.error('[ConversationStorage] Error saving conversation:', error);
        return throwError(() => new Error('Failed to save conversation'));
      })
    );
  }

  /**
   * Get a conversation by ID
   */
  getConversation(id: string): Observable<Conversation | null> {
    return from(this.db.getItem<Conversation>(id)).pipe(
      map(conversation => conversation || null),
      catchError(error => {
        console.error('[ConversationStorage] Error getting conversation:', error);
        return of(null);
      })
    );
  }

  /**
   * Get all conversations
   */
  getAllConversations(): Observable<Conversation[]> {
    return from(this.db.keys()).pipe(
      switchMap(keys => {
        const conversationPromises = keys.map(key =>
          this.db.getItem<Conversation>(key as string)
        );
        return from(Promise.all(conversationPromises));
      }),
      map(conversations => {
        const validConversations = conversations
          .filter((c): c is Conversation => c !== null)
          .sort((a, b) => b.updatedAt.getTime() - a.updatedAt.getTime())
          .slice(0, this.maxConversations);
        return validConversations;
      }),
      catchError(error => {
        console.error('[ConversationStorage] Error getting all conversations:', error);
        return of([]);
      })
    );
  }

  /**
   * Delete a conversation
   */
  deleteConversation(id: string): Observable<void> {
    return from(this.db.removeItem(id)).pipe(
      catchError(error => {
        console.error('[ConversationStorage] Error deleting conversation:', error);
        return throwError(() => new Error('Failed to delete conversation'));
      })
    );
  }

  /**
   * Delete all conversations
   */
  deleteAllConversations(): Observable<void> {
    return from(this.db.clear()).pipe(
      catchError(error => {
        console.error('[ConversationStorage] Error clearing conversations:', error);
        return throwError(() => new Error('Failed to clear conversations'));
      })
    );
  }

  /**
   * Search conversations by text
   */
  searchConversations(query: string): Observable<Conversation[]> {
    return this.getAllConversations().pipe(
      map(conversations => {
        const lowerQuery = query.toLowerCase();
        return conversations.filter(conv =>
          conv.title.toLowerCase().includes(lowerQuery) ||
          conv.messages.some(msg => msg.text.toLowerCase().includes(lowerQuery))
        );
      })
    );
  }

  /**
   * Get current session ID
   */
  getCurrentSessionId(): Observable<string | null> {
    return from(this.db.getItem<string>(this.currentSessionKey)).pipe(
      map(id => id || null),
      catchError(() => of(null))
    );
  }
// Set current session ID
  setCurrentSessionId(sessionId: string): Observable<void> {
    return from(this.db.setItem(this.currentSessionKey, sessionId)).pipe(
      map(() => void 0),
      catchError(error => {
        console.error('[ConversationStorage] Error setting session ID:', error);
        return throwError(() => new Error('Failed to set session ID'));
      })
    );
  }
// Update conversation title
    updateConversationTitle(id: string, title: string): Observable<Conversation | null> {
    return this.getConversation(id).pipe(
      switchMap(conversation => {
        if (!conversation) return of(null);
        conversation.title = title;
        conversation.updatedAt = new Date();
        return this.saveConversation(conversation);
      })
    );
  }

  /**
   * Get storage usage statistics
   */
  getStorageStats(): Observable<{ count: number; totalSize: number }> {
    return from(this.db.keys()).pipe(
      switchMap(keys => {
        const sizePromises = keys.map(key =>
          this.db.getItem(key).then(item => JSON.stringify(item).length)
        );
        return Promise.all([Promise.resolve(keys.length), Promise.all(sizePromises)]);
      }),
      map(([count, sizes]) => ({
        count,
        totalSize: sizes.reduce((sum, size) => sum + size, 0)
      })),
      catchError(() => of({ count: 0, totalSize: 0 }))
    );
  }

  /**
   * Export conversations as JSON
   */
  exportConversationsAsJSON(): Observable<string> {
    return this.getAllConversations().pipe(
      map(conversations => JSON.stringify(conversations, null, 2))
    );
  }

  /**
   * Import conversations from JSON
   */
  importConversationsFromJSON(json: string): Observable<number> {
    try {
      const conversations = JSON.parse(json) as Conversation[];
      const importPromises = conversations.map(conv =>
        this.saveConversation(conv).toPromise()
      );
      return from(Promise.all(importPromises)).pipe(
        map(() => conversations.length)
      );
    } catch (error) {
      return throwError(() => new Error('Invalid JSON format'));
    }
  }
}
