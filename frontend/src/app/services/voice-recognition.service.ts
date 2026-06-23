import { Injectable, NgZone } from '@angular/core';
import { Observable, Subject, fromEvent, of } from 'rxjs';
import { map, filter, takeUntil } from 'rxjs/operators';

export interface VoiceRecognitionConfig {
  lang?: string;
  continuous?: boolean;
  interimResults?: boolean;
  maxAlternatives?: number;
}

export interface VoiceRecognitionResult {
  transcript: string;
  confidence: number;
  isFinal: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class VoiceRecognitionService {
  private recognition: any = null;
  private isListening = false;
  private destroy$ = new Subject<void>();
  private recognitionSupported = false;

  constructor(private ngZone: NgZone) {
    this.checkSupport();
  }

  /**
   * Check if speech recognition is supported
   */
  private checkSupport(): void {
    this.recognitionSupported = !!(window as any).SpeechRecognition || !!(window as any).webkitSpeechRecognition;
    
    if (this.recognitionSupported) {
      const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      this.recognition = new SpeechRecognition();
      this.setupRecognitionDefaults();
    }
  }

  /**
   * Setup default recognition settings
   */
  private setupRecognitionDefaults(): void {
    this.recognition.continuous = false;
    this.recognition.interimResults = true;
    this.recognition.lang = 'fr-FR';
    this.recognition.maxAlternatives = 1;
  }

  /**
   * Check if voice recognition is available
   */
  isAvailable(): boolean {
    return this.recognitionSupported;
  }

  /**
   * Start voice recognition
   */
  start(config: VoiceRecognitionConfig = {}): Observable<VoiceRecognitionResult> {
    if (!this.recognitionSupported) {
      return of({ transcript: 'Voice recognition not supported', confidence: 0, isFinal: true });
    }

    if (this.isListening) {
      this.stop();
    }

    this.applyConfig(config);
    this.recognition.start();
    this.isListening = true;

    return new Observable<VoiceRecognitionResult>(observer => {
      const onResult = (event: any) => {
        this.ngZone.run(() => {
          const result = this.processRecognitionEvent(event);
          if (result) {
            observer.next(result);
          }
        });
      };

      const onError = (event: any) => {
        this.ngZone.run(() => {
          console.error('[VoiceRecognition] Error:', event.error);
          this.isListening = false;
          observer.error({
            transcript: '',
            confidence: 0,
            isFinal: true,
            error: event.error
          });
          observer.complete();
        });
      };

      const onEnd = () => {
        this.ngZone.run(() => {
          this.isListening = false;
          observer.complete();
        });
      };

      this.recognition.addEventListener('result', onResult);
      this.recognition.addEventListener('error', onError);
      this.recognition.addEventListener('end', onEnd);

      return () => {
        this.recognition.removeEventListener('result', onResult);
        this.recognition.removeEventListener('error', onError);
        this.recognition.removeEventListener('end', onEnd);
        if (this.isListening) {
          this.stop();
        }
      };
    }).pipe(takeUntil(this.destroy$));
  }

  /**
   * Stop voice recognition
   */
  stop(): void {
    if (this.recognition && this.isListening) {
      this.recognition.stop();
      this.isListening = false;
    }
  }

  /**
   * Apply configuration to recognition
   */
  private applyConfig(config: VoiceRecognitionConfig): void {
    if (config.lang) {
      this.recognition.lang = config.lang;
    }
    if (config.continuous !== undefined) {
      this.recognition.continuous = config.continuous;
    }
    if (config.interimResults !== undefined) {
      this.recognition.interimResults = config.interimResults;
    }
    if (config.maxAlternatives !== undefined) {
      this.recognition.maxAlternatives = config.maxAlternatives;
    }
  }

  /**
   * Process recognition event
   */
  private processRecognitionEvent(event: any): VoiceRecognitionResult | null {
    const result = event.results[event.results.length - 1];
    
    if (!result) {
      return null;
    }

    const transcript = result[0].transcript;
    const confidence = result[0].confidence;
    const isFinal = result.isFinal;

    return {
      transcript,
      confidence,
      isFinal
    };
  }

  /**
   * Get available languages
   */
  getAvailableLanguages(): string[] {
    const commonLanguages = [
      { code: 'fr-FR', name: 'French (France)' },
      { code: 'en-US', name: 'English (US)' },
      { code: 'en-GB', name: 'English (UK)' },
      { code: 'es-ES', name: 'Spanish (Spain)' },
      { code: 'de-DE', name: 'German (Germany)' },
      { code: 'it-IT', name: 'Italian (Italy)' },
      { code: 'ar-SA', name: 'Arabic (Saudi Arabia)' }
    ];
    return commonLanguages.map(lang => lang.code);
  }

  /**
   * Detect language from browser
   */
  detectBrowserLanguage(): string {
    const browserLang = navigator.language || 'fr-FR';
    return browserLang;
  }

  /**
   * Check microphone permission
   */
  async checkMicrophonePermission(): Promise<boolean> {
    try {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        await navigator.mediaDevices.getUserMedia({ audio: true });
        return true;
      }
      return false;
    } catch (error) {
      console.error('[VoiceRecognition] Microphone permission denied:', error);
      return false;
    }
  }

  /**
   * Request microphone permission
   */
  async requestMicrophonePermission(): Promise<boolean> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      // Stop all tracks to release the microphone
      stream.getTracks().forEach(track => track.stop());
      return true;
    } catch (error) {
      console.error('[VoiceRecognition] Failed to request microphone permission:', error);
      return false;
    }
  }

  /**
   * Get current listening state
   */
  getListeningState(): boolean {
    return this.isListening;
  }

  /**
   * Process voice commands
   */
  processCommand(transcript: string): { command: string; params: string[] } | null {
    const lowerTranscript = transcript.toLowerCase().trim();
    
    const commands = {
      'effacer': { command: 'clear', params: [] },
      'supprimer': { command: 'clear', params: [] },
      'nouvelle conversation': { command: 'new', params: [] },
      'exporter': { command: 'export', params: [] },
      'imprimer': { command: 'print', params: [] },
      'rechercher': { command: 'search', params: this.extractSearchQuery(lowerTranscript) },
      'aide': { command: 'help', params: [] },
      'annuler': { command: 'cancel', params: [] }
    };

    for (const [key, value] of Object.entries(commands)) {
      if (lowerTranscript.includes(key)) {
        return value;
      }
    }

    return null;
  }

  /**
   * Extract search query from transcript
   */
  private extractSearchQuery(transcript: string): string[] {
    const match = transcript.match(/rechercher\s+(.+)/i);
    return match ? [match[1].trim()] : [];
  }

  /**
   * Cleanup on destroy
   */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.stop();
  }
}
