import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface MessageExchange {
  role: 'user';
  content: string;
  timestamp: number;
}

export interface SessionMemoryResponse {
  success: boolean;
  sessionId: string;
  context: Record<string, unknown>;
}

export interface MessageHistoryResponse {
  success: boolean;
  sessionId: string;
  messages: MessageExchange[];
}

export interface NewSessionResponse {
  success: boolean;
  sessionId: string;
}

@Injectable({
  providedIn: 'root'
})
export class MemoryService {
  private readonly baseUrl = `${environment.apiProduit}`;
  private readonly memoryUrl = `${this.baseUrl}/memory`;

  constructor(private readonly http: HttpClient) {}

  getSessionMemory(sessionId: string): Observable<SessionMemoryResponse> {
    return this.http.get<SessionMemoryResponse>(`${this.memoryUrl}/session/${sessionId}`).pipe(
      catchError((error) =>
        of({
          success: false,
          sessionId,
          context: {}
        } as SessionMemoryResponse)
      )
    );
  }

  getMessageHistory(sessionId: string, limit: number = 10): Observable<MessageHistoryResponse> {
    return this.http
      .get<MessageHistoryResponse>(`${this.memoryUrl}/session/${sessionId}/messages`, {
        params: { limit: limit.toString() }
      })
      .pipe(
        catchError((error) =>
          of({
            success: false,
            sessionId,
            messages: []
          } as MessageHistoryResponse)
        )
      );
  }

  recordFeedback(sessionId: string, feedback: string): Observable<{ success: boolean; message: string }> {
    return this.http
      .post<{ success: boolean; message: string }>(`${this.memoryUrl}/session/${sessionId}/feedback`, {
        feedback
      })
      .pipe(
        catchError((error) =>
          of({
            success: false,
            message: error.error?.error || error.message || 'Erreur lors de l\'enregistrement du feedback'
          })
        )
      );
  }

  updatePreferences(sessionId: string, preferences: Record<string, unknown>): Observable<{ success: boolean; message: string }> {
    return this.http
      .post<{ success: boolean; message: string }>(`${this.memoryUrl}/session/${sessionId}/preferences`, preferences)
      .pipe(
        catchError((error) =>
          of({
            success: false,
            message: error.error?.error || error.message || 'Erreur lors de la mise à jour des préférences'
          })
        )
      );
  }

  deleteSession(sessionId: string): Observable<{ success: boolean; message: string }> {
    return this.http.delete<{ success: boolean; message: string }>(`${this.memoryUrl}/session/${sessionId}`).pipe(
      catchError((error) =>
        of({
          success: false,
          message: error.error?.error || error.message || 'Erreur lors de la suppression de la session'
        })
      )
    );
  }

  cleanupOldMemories(): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.memoryUrl}/cleanup`, {}).pipe(
      catchError((error) =>
        of({
          success: false,
          message: error.error?.error || error.message || 'Erreur lors du nettoyage des mémoires'
        })
      )
    );
  }

  generateNewSession(): Observable<NewSessionResponse> {
    return this.http.get<NewSessionResponse>(`${this.memoryUrl}/session/new`).pipe(
      catchError((error) =>
        of({
          success: false,
          sessionId: ''
        } as NewSessionResponse)
      )
    );
  }
}
