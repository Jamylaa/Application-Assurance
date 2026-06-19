import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface RAGSearchResult {
  id: string;
  type: string;
  content: string;
  score: number;
  metadata?: Record<string, unknown>;
}

export interface RAGSearchResponse {
  success: boolean;
  query: string;
  results: RAGSearchResult[];
  count: number;
}

export interface EmbeddingResponse {
  success: boolean;
  text: string;
  embedding: number[];
  dimension: number;
}

@Injectable({
  providedIn: 'root'
})
export class RAGService {
  private readonly baseUrl = `${environment.apiProduit}`;
  private readonly ragUrl = `${this.baseUrl}/rag`;

  constructor(private readonly http: HttpClient) {}

  search(query: string, documentType?: string): Observable<RAGSearchResponse> {
    const body: Record<string, string> = { query };
    if (documentType) {
      body['documentType'] = documentType;
    }
    return this.http.post<RAGSearchResponse>(`${this.ragUrl}/search`, body).pipe(
      catchError((error) =>
        of({
          success: false,
          query,
          results: [],
          count: 0
        } as RAGSearchResponse)
      )
    );
  }

  searchWithFallback(query: string): Observable<RAGSearchResponse> {
    return this.http.post<RAGSearchResponse>(`${this.ragUrl}/search/fallback`, { query }).pipe(
      catchError((error) =>
        of({
          success: false,
          query,
          results: [],
          count: 0
        } as RAGSearchResponse)
      )
    );
  }

  indexAll(): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.ragUrl}/index`, {}).pipe(
      catchError((error) =>
        of({
          success: false,
          message: error.error?.error || error.message || 'Erreur lors de l\'indexation'
        })
      )
    );
  }

  reindexEntity(documentType: string, entityId: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.ragUrl}/index/${documentType}/${entityId}`,
      {}
    ).pipe(
      catchError((error) =>
        of({
          success: false,
          message: error.error?.error || error.message || 'Erreur lors de la réindexation'
        })
      )
    );
  }

  generateEmbedding(text: string): Observable<EmbeddingResponse> {
    return this.http.post<EmbeddingResponse>(`${this.ragUrl}/embedding`, { text }).pipe(
      catchError((error) =>
        of({
          success: false,
          text,
          embedding: [],
          dimension: 0
        } as EmbeddingResponse)
      )
    );
  }
}
