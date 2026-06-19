import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Garantie, Pack, PackGarantie, Produit } from '../models/entities.model';

export enum ChatbotIntent {
  CREATE_PRODUIT = 'CREATE_PRODUIT',
  CREATE_GARANTIE = 'CREATE_GARANTIE',
  CREATE_PACK = 'CREATE_PACK',
  CONFIGURE_PACK = 'CONFIGURE_PACK',
  CREATE_PACK_WITH_GARANTIES = 'CREATE_PACK_WITH_GARANTIES',
  UPDATE_PRODUIT = 'UPDATE_PRODUIT',
  UPDATE_GARANTIE = 'UPDATE_GARANTIE',
  UPDATE_PACK = 'UPDATE_PACK',
  DELETE_PRODUIT = 'DELETE_PRODUIT',
  DELETE_GARANTIE = 'DELETE_GARANTIE',
  DELETE_PACK = 'DELETE_PACK',
  LIST_PRODUITS = 'LIST_PRODUITS',
  LIST_GARANTIES = 'LIST_GARANTIES',
  LIST_PACKS = 'LIST_PACKS',
  RECOMMANDATION = 'RECOMMANDATION',
  HELP = 'HELP',
  UNKNOWN = 'UNKNOWN'
}

export interface ChatbotRequest {
  prompt: string;
  sessionId?: string;
  context?: {
    previousIntents?: ChatbotIntent[];
    entities?: Record<string, unknown>;
  };
}

export interface ChatbotResponse {
  success: boolean;
  intent: ChatbotIntent;
  confidence: number;
  message: string;
  data?: {
    produit?: Produit;
    garantie?: Garantie;
    pack?: Pack;
    packGarantie?: PackGarantie;
    produits?: Produit[];
    garanties?: Garantie[];
    packs?: Pack[];
    packWithGaranties?: {
      pack: Pack;
      garanties: Array<{ garantie: Garantie; packGarantie: PackGarantie }>;
    };
    recommendations?: unknown[];
    explanation?: string;
  };
  validation?: {
    valid: boolean;
    errors: string[];
    warnings: string[];
  };
  actions?: Array<{
    type: 'CREATE' | 'UPDATE' | 'DELETE' | 'NAVIGATE' | 'LIST';
    label: string;
    data?: unknown;
    route?: string[];
  }>;
  error?: string;
  timestamp: number;
}

export interface ChatbotResponseDTO {
  success: boolean;
  action?: string;
  result?: unknown;
  prompt?: string;
  message?: string;
  errors?: string[];
  warnings?: string[];
  timestamp?: number;
  data?: Record<string, unknown>;
}

@Injectable({
  providedIn: 'root'
})
export class ChatbotService {
  private readonly baseUrl = `${environment.apiProduit}`;
  private readonly chatbotUrl = `${this.baseUrl}/chatbot`;
  private currentSessionId: string | null = null;

  constructor(private readonly http: HttpClient) {}

  processPrompt(request: ChatbotRequest): Observable<ChatbotResponse> {
    const requestBody: { prompt: string; sessionId?: string } = { prompt: request.prompt };

    if (request.sessionId) {
      requestBody.sessionId = request.sessionId;
    } else if (this.currentSessionId) {
      requestBody.sessionId = this.currentSessionId;
    }

    return this.http.post<ChatbotResponseDTO>(`${this.chatbotUrl}/process`, requestBody).pipe(
      map((dto) => {
        const response = this.mapDtoToChatbotResponse(dto);
        if (dto.data?.['sessionId'] && typeof dto.data['sessionId'] === 'string') {
          this.currentSessionId = dto.data['sessionId'];
        }
        return response;
      }),
      catchError((error) =>
        of({
          success: false,
          intent: ChatbotIntent.UNKNOWN,
          confidence: 0,
          message: 'Erreur de communication avec le serveur chatbot',
          error: this.extractErrorMessage(error),
          validation: {
            valid: false,
            errors: [error.error?.message || error.message || 'Erreur réseau'],
            warnings: []
          },
          timestamp: Date.now()
        })
      )
    );
  }

  healthCheck(): Observable<{ status: string; timestamp: number }> {
    return this.http.get<Record<string, unknown>>(`${this.chatbotUrl}/health`).pipe(
      map((body) => ({
        status: String(body['status'] ?? 'UNKNOWN'),
        timestamp: typeof body['timestamp'] === 'number' ? (body['timestamp'] as number) : Date.now()
      })),
      catchError(() =>
        of({
          status: 'UNHEALTHY',
          timestamp: Date.now()
        })
      )
    );
  }

  getSupportedActions(): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.chatbotUrl}/actions`);
  }

  getAIStatus(): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.chatbotUrl}/ai-status`);
  }

  testController(): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.chatbotUrl}/test`);
  }

  callBackend(prompt: string): Observable<ChatbotResponse> {
    return this.processPrompt({ prompt, sessionId: this.currentSessionId || undefined });
  }

  resetSession(): void {
    this.currentSessionId = null;
  }

  getCurrentSessionId(): string | null {
    return this.currentSessionId;
  }

  private extractErrorMessage(error: unknown): string {
    const err = error as any;
    return (
      err?.error?.message ||
      err?.details?.message ||
      err?.details?.error ||
      (Array.isArray(err?.details?.errors) ? err.details.errors.join('\n') : undefined) ||
      err?.message ||
      err?.statusText ||
      'Erreur'
    );
  }

  private mapActionToIntent(action?: string): ChatbotIntent {
    switch ((action || '').toUpperCase()) {
      case 'GARANTIE':
        return ChatbotIntent.CREATE_GARANTIE;
      case 'PRODUIT':
        return ChatbotIntent.CREATE_PRODUIT;
      case 'PACK':
        return ChatbotIntent.CREATE_PACK;
      case 'CONFIGURATION_PACK':
        return ChatbotIntent.CONFIGURE_PACK;
      case 'AJOUT_GARANTIE_PACK':
        return ChatbotIntent.CONFIGURE_PACK;
      case 'RECOMMANDATION':
        return ChatbotIntent.RECOMMANDATION;
      default:
        return ChatbotIntent.UNKNOWN;
    }
  }

  private mapDtoToChatbotResponse(dto: ChatbotResponseDTO): ChatbotResponse {
    const intent = this.mapActionToIntent(dto.action);
    const ts = dto.timestamp ?? Date.now();

    if (!dto.success) {
      // Vérifier s'il y a des données utiles même en cas d'échec
      const result = dto.result as Record<string, unknown> | undefined;
      const hasUsefulData = result && (
        result['result'] ||
        result['recommendedPacks'] ||
        result['entity'] ||
        result['profileProcessed']
      );

      if (hasUsefulData) {
        console.warn('[ChatbotService] Backend returned success=false but has useful data, treating as partial success');
        return this.buildSuccessResponse(dto, intent, ts, true);
      }

      return {
        success: false,
        intent,
        confidence: 0,
        message: dto.message || dto.errors?.join('\n') || 'Échec du traitement',
        validation: {
          valid: false,
          errors: dto.errors?.length ? dto.errors : [dto.message || 'Erreur'],
          warnings: dto.warnings || []
        },
        timestamp: ts
      };
    }

    return this.buildSuccessResponse(dto, intent, ts, false);
  }

  private buildSuccessResponse(dto: ChatbotResponseDTO, intent: ChatbotIntent, ts: number, isPartial: boolean): ChatbotResponse {
    const raw = dto.result;
    if (!raw || typeof raw !== 'object') {
      return {
        success: true,
        intent,
        confidence: 1,
        message: isPartial ?
          (dto.message || 'Traitement partiel terminé (certains services ont utilisé le fallback)') :
          (dto.message || 'Traitement terminé'),
        timestamp: ts
      };
    }

    const result = raw as Record<string, unknown>;
    if (result['success'] === false) {
      const details = result['details'];
      const errList = Array.isArray(details)
        ? (details as unknown[]).map(String)
        : [String(result['error'] || result['message'] || 'Erreur métier')];

      // Vérifier s'il y a des données malgré l'erreur
      const hasData = result['result'] || result['recommendedPacks'] || result['entity'] || result['profileProcessed'];

      if (hasData) {
        console.warn('[ChatbotService] Result success=false but has data, treating as partial success');
        return this.buildSuccessResponseFromResult(result, dto, intent, ts, true);
      }

      return {
        success: false,
        intent,
        confidence: 0,
        message: String(result['error'] || result['message'] || 'Échec'),
        validation: {
          valid: false,
          errors: errList,
          warnings: (result['warnings'] as string[]) || dto.warnings || []
        },
        timestamp: ts
      };
    }

    return this.buildSuccessResponseFromResult(result, dto, intent, ts, isPartial);
  }

  private buildSuccessResponseFromResult(result: Record<string, unknown>, dto: ChatbotResponseDTO, intent: ChatbotIntent, ts: number, isPartial: boolean): ChatbotResponse {
    const innerAction = String(result['action'] || dto.action || '');
    const entity = result['entity'];
    const data: ChatbotResponse['data'] = {};
    if (entity && typeof entity === 'object') {
      if (
        innerAction === 'CONFIGURE_PACK' ||
        innerAction === 'ADD_GARANTIE_TO_PACK' ||
        innerAction.includes('PACK_GARANTIE')
      ) {
        data.packGarantie = entity as PackGarantie;
      } else if (innerAction.includes('PRODUIT')) {
        data.produit = entity as Produit;
      } else if (innerAction.includes('GARANTIE')) {
        data.garantie = entity as Garantie;
      } else if (innerAction.includes('PACK')) {
        data.pack = entity as Pack;
      } else if (dto.action === 'PRODUIT') {
        data.produit = entity as Produit;
      } else if (dto.action === 'GARANTIE') {
        data.garantie = entity as Garantie;
      } else if (dto.action === 'PACK') {
        data.pack = entity as Pack;
      } else if (dto.action === 'CONFIGURATION_PACK' || dto.action === 'AJOUT_GARANTIE_PACK') {
        data.packGarantie = entity as PackGarantie;
      }
    }

    // Handle RECOMMANDATION separately since it doesn't use entity
    if (dto.action === 'RECOMMANDATION') {
      const resultData = result['result'] as Record<string, unknown> | undefined;
      data.recommendations = (resultData?.['recommendedPacks'] || result['recommendedPacks'] || result['recommendations'] || result['packs']) as unknown[];
      const explanation = resultData?.['explanation'] || result['explanation'];
      data.explanation = typeof explanation === 'string' ? explanation : undefined;
      console.log('[ChatbotService] RECOMMANDATION - extracted recommendations:', data.recommendations);
      console.log('[ChatbotService] RECOMMANDATION - extracted explanation:', data.explanation);
    }

    const message = isPartial ?
      (String(result['message'] || dto.message) + ' (traité avec fallback)') :
      String(result['message'] || dto.message || 'Opération réussie');
    const warnings = (result['warnings'] as string[]) || dto.warnings || [];

    const actions: ChatbotResponse['actions'] = [];
    if (data.produit?.idProduit) {
      actions.push({ type: 'NAVIGATE', label: 'Voir le produit', route: ['/produits'] });
    }
    if (data.garantie?.idGarantie) {
      actions.push({ type: 'NAVIGATE', label: 'Voir la garantie', route: ['/garanties'] });
    }
    if (data.pack?.idPack) {
      actions.push({ type: 'NAVIGATE', label: 'Voir le pack', route: ['/packs'] });
    }

    return {
      success: true,
      intent,
      confidence: isPartial ? 0.8 : 1,
      message,
      data: Object.keys(data).length ? data : undefined,
      validation: { valid: true, errors: [], warnings },
      actions: actions.length ? actions : undefined,
      timestamp: ts
    };
  }
}
