import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface AnalyticsEvent {
  eventName: string;
  category: string;
  action?: string;
  label?: string;
  value?: number;
  timestamp: Date;
  sessionId?: string;
  userId?: string;
  metadata?: Record<string, any>;
  context?: string;
}
export interface UserSession {
  sessionId: string;
  startTime: Date;
  endTime?: Date;
  messageCount: number;
  intents: string[];
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private events: AnalyticsEvent[] = [];
  private currentSession: UserSession | null = null;
  private sessionId: string;
  private maxEvents = 1000;

  constructor() {
    this.sessionId = this.generateSessionId();
    this.startSession();
  }

  /**
   * Generate unique session ID
   */
  private generateSessionId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Start a new session
   */
  startSession(): void {
    this.currentSession = {
      sessionId: this.sessionId,
      startTime: new Date(),
      messageCount: 0,
      intents: []
    };
  }

  /**
   * End current session
   */
  endSession(): void {
    if (this.currentSession) {
      this.currentSession.endTime = new Date();
      this.currentSession.duration = 
        this.currentSession.endTime.getTime() - this.currentSession.startTime.getTime();
    }
  }

  /**
   * Track an event
   */
  trackEvent(
    eventName: string,
    category: string,
    action?: string,
    label?: string,
    value?: number,
    metadata?: Record<string, any>
  ): void {
    const event: AnalyticsEvent = {
      eventName,
      category,
      action,
      label,
      value,
      timestamp: new Date(),
      sessionId: this.sessionId,
      metadata
    };

    this.events.push(event);

    // Keep only recent events
    if (this.events.length > this.maxEvents) {
      this.events = this.events.slice(-this.maxEvents);
    }

    console.log('[Analytics]', event);
  }

  /**
   * Track message sent
   */
  trackMessageSent(message: string, intent?: string): void {
    this.trackEvent(
      'message_sent',
      'chatbot',
      'send',
      intent,
      message.length,
      { intent, messageLength: message.length }
    );

    if (this.currentSession) {
      this.currentSession.messageCount++;
      if (intent) {
        this.currentSession.intents.push(intent);
      }
    }
  }

  /**
   * Track message received
   */
  trackMessageReceived(intent?: string, success: boolean = true): void {
    this.trackEvent(
      'message_received',
      'chatbot',
      'receive',
      intent,
      success ? 1 : 0,
      { intent, success }
    );
  }

  /**
   * Track intent
   */
  trackIntent(intent: string, confidence: number): void {
    this.trackEvent(
      'intent_detected',
      'chatbot',
      'intent',
      intent,
      Math.round(confidence * 100),
      { confidence }
    );
  }

  /**
   * Track error
   */
  trackError(error: string, context?: string): void {
    this.trackEvent(
      'error',
      'system',
      'error',
      context,
      undefined,
      { error, context }
    );
  }

  /**
   * Track user action
   */
  trackUserAction(action: string, element?: string): void {
    this.trackEvent(
      'user_action',
      'ui',
      'click',
      element,
      undefined,
      { action, element }
    );
  }

  /**
   * Track search
   */
  trackSearch(query: string, resultsCount: number): void {
    this.trackEvent(
      'search',
      'navigation',
      'search',
      query,
      resultsCount,
      { query, resultsCount }
    );
  }

  /**
   * Track export
   */
  trackExport(format: string, itemCount: number): void {
    this.trackEvent(
      'export',
      'data',
      'export',
      format,
      itemCount,
      { format, itemCount }
    );
  }

  /**
   * Track voice input usage
   */
  trackVoiceInput(duration: number, success: boolean): void {
    this.trackEvent(
      'voice_input',
      'input',
      'voice',
      success ? 'success' : 'error',
      Math.round(duration / 1000),
      { duration, success }
    );
  }

  /**
   * Track suggestion click
   */
  trackSuggestionClick(suggestion: string): void {
    this.trackEvent(
      'suggestion_click',
      'chatbot',
      'click',
      suggestion,
      undefined,
      { suggestion }
    );
  }

  /**
   * Track page view
   */
  trackPageView(page: string): void {
    this.trackEvent(
      'page_view',
      'navigation',
      'view',
      page,
      undefined,
      { page }
    );
  }

  /**
   * Get all events
   */
  getEvents(): Observable<AnalyticsEvent[]> {
    return of(this.events);
  }

  /**
   * Get events by category
   */
  getEventsByCategory(category: string): Observable<AnalyticsEvent[]> {
    return of(this.events.filter(e => e.category === category));
  }

  /**
   * Get events by name
   */
  getEventsByName(eventName: string): Observable<AnalyticsEvent[]> {
    return of(this.events.filter(e => e.eventName === eventName));
  }

  /**
   * Get events in date range
   */
  getEventsInDateRange(start: Date, end: Date): Observable<AnalyticsEvent[]> {
    return of(
      this.events.filter(e => e.timestamp >= start && e.timestamp <= end)
    );
  }

  /**
   * Get current session stats
   */
  getSessionStats(): Observable<UserSession | null> {
    return of(this.currentSession);
  }

  /**
   * Get intent statistics
   */
  getIntentStats(): Observable<Record<string, number>> {
    const stats: Record<string, number> = {};
    
    this.events
      .filter(e => e.eventName === 'intent_detected' || e.eventName === 'message_sent')
      .forEach(e => {
        const intent = e.label || 'unknown';
        stats[intent] = (stats[intent] || 0) + 1;
      });

    return of(stats);
  }

  /**
   * Get error statistics
   */
  getErrorStats(): Observable<Record<string, number>> {
    const stats: Record<string, number> = {};
    
    this.events
      .filter(e => e.eventName === 'error')
      .forEach(e => {
        const context = e.context || 'unknown';
        stats[context] = (stats[context] || 0) + 1;
      });

    return of(stats);
  }

  /**
   * Export analytics data
   */
  exportAnalytics(): Observable<string> {
    const data = {
      sessionId: this.sessionId,
      session: this.currentSession,
      events: this.events,
      exportDate: new Date().toISOString()
    };
    
    return of(JSON.stringify(data, null, 2));
  }

  /**
   * Clear all events
   */
  clearEvents(): void {
    this.events = [];
  }

  /**
   * Send analytics to external service (placeholder)
   */
  sendToExternalService(endpoint: string): Observable<boolean> {
    return of(true);
  }

  /**
   * Get performance metrics
   */
  getPerformanceMetrics(): Observable<any> {
    if (typeof performance !== 'undefined') {
      const perf = performance as any;
      return of({
        memory: perf.memory ? {
          usedJSHeapSize: perf.memory.usedJSHeapSize,
          totalJSHeapSize: perf.memory.totalJSHeapSize,
          jsHeapSizeLimit: perf.memory.jsHeapSizeLimit
        } : null,
        timing: perf.timing ? {
          domContentLoaded: perf.timing.domContentLoadedEventEnd - perf.timing.navigationStart,
          loadComplete: perf.timing.loadEventEnd - perf.timing.navigationStart
        } : null
      });
    }
    return of(null);
  }
}
