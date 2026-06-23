import { ChatMessage } from '../services/conversation-storage.service';

/**
 * Format message text with markdown-like syntax
 */
export function formatMessageText(text: string): string {
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
    .replace(/•/g, '&bull;');
}

/**
 * Truncate text to specified length with ellipsis
 */
export function truncateText(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength - 3) + '...';
}

/**
 * Generate unique ID
 */
export function generateId(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Format timestamp to readable time
 */
export function formatTime(date: Date): string {
  return date.toLocaleTimeString('fr-FR', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

/**
 * Format timestamp to readable date and time
 */
export function formatDateTime(date: Date): string {
  return date.toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

/**
 * Get relative time (e.g., "2 hours ago")
 */
export function getRelativeTime(date: Date): string {
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (seconds < 60) {
    return 'à l\'instant';
  } else if (minutes < 60) {
    return `il y a ${minutes} min`;
  } else if (hours < 24) {
    return `il y a ${hours} h`;
  } else if (days < 7) {
    return `il y a ${days} j`;
  } else {
    return formatDateTime(date);
  }
}

/**
 * Extract intent from message
 */
export function extractIntent(message: ChatMessage): string | null {
  return message.intent || null;
}

/**
 * Check if message is from user
 */
export function isUserMessage(message: ChatMessage): boolean {
  return message.sender === 'user';
}

/**
 * Check if message is from bot
 */
export function isBotMessage(message: ChatMessage): boolean {
  return message.sender === 'bot';
}

/**
 * Filter messages by intent
 */
export function filterByIntent(messages: ChatMessage[], intent: string): ChatMessage[] {
  return messages.filter(msg => msg.intent === intent);
}

/**
 * Filter messages by date range
 */
export function filterByDateRange(
  messages: ChatMessage[],
  startDate: Date,
  endDate: Date
): ChatMessage[] {
  return messages.filter(msg => {
    const msgDate = new Date(msg.timestamp);
    return msgDate >= startDate && msgDate <= endDate;
  });
}

/**
 * Get message count
 */
export function getMessageCount(messages: ChatMessage[]): number {
  return messages.length;
}

/**
 * Get user message count
 */
export function getUserMessageCount(messages: ChatMessage[]): number {
  return messages.filter(msg => msg.sender === 'user').length;
}

/**
 * Get bot message count
 */
export function getBotMessageCount(messages: ChatMessage[]): number {
  return messages.filter(msg => msg.sender === 'bot').length;
}

/**
 * Group messages by date
 */
export function groupMessagesByDate(messages: ChatMessage[]): Map<string, ChatMessage[]> {
  const grouped = new Map<string, ChatMessage[]>();
  
  messages.forEach(msg => {
    const date = new Date(msg.timestamp).toDateString();
    if (!grouped.has(date)) {
      grouped.set(date, []);
    }
    grouped.get(date)!.push(msg);
  });
  
  return grouped;
}

/**
 * Search messages by text
 */
export function searchMessages(messages: ChatMessage[], query: string): ChatMessage[] {
  const lowerQuery = query.toLowerCase();
  return messages.filter(msg => 
    msg.text.toLowerCase().includes(lowerQuery)
  );
}

/**
 * Highlight search terms in text
 */
export function highlightSearchTerms(text: string, query: string): string {
  if (!query.trim()) return text;
  
  const regex = new RegExp(`(${escapeRegex(query)})`, 'gi');
  return text.replace(regex, '<mark>$1</mark>');
}

/**
 * Escape special regex characters
 */
export function escapeRegex(string: string): string {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Sanitize HTML to prevent XSS
 */
export function sanitizeHTML(html: string): string {
  const temp = document.createElement('div');
  temp.textContent = html;
  return temp.innerHTML;
}

/**
 * Detect language from text (simple implementation)
 */
export function detectLanguage(text: string): string {
  // Simple language detection based on common words
  const frenchWords = ['le', 'la', 'les', 'un', 'une', 'des', 'et', 'ou', 'mais', 'pour', 'avec'];
  const englishWords = ['the', 'a', 'an', 'and', 'or', 'but', 'for', 'with'];
  
  const lowerText = text.toLowerCase();
  const frenchCount = frenchWords.filter(word => lowerText.includes(word)).length;
  const englishCount = englishWords.filter(word => lowerText.includes(word)).length;
  
  if (frenchCount > englishCount) return 'fr';
  if (englishCount > frenchCount) return 'en';
  return 'fr'; // Default to French
}

/**
 * Calculate reading time
 */
export function calculateReadingTime(text: string): number {
  const wordsPerMinute = 200;
  const wordCount = text.split(/\s+/).length;
  return Math.ceil(wordCount / wordsPerMinute);
}

/**
 * Extract entities from text (simple implementation)
 */
export function extractEntities(text: string): Record<string, string[]> {
  const entities: Record<string, string[]> = {
    numbers: [],
    emails: [],
    urls: []
  };
  
  // Extract numbers
  const numberMatches = text.match(/\d+/g);
  if (numberMatches) {
    entities['numbers'] = numberMatches;
  }
  
  // Extract emails
  const emailMatches = text.match(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g);
  if (emailMatches) {
    entities['emails'] = emailMatches;
  }
  
  // Extract URLs
  const urlMatches = text.match(/https?:\/\/[^\s]+/g);
  if (urlMatches) {
    entities['urls'] = urlMatches;
  }
  
  return entities;
}

/**
 * Validate message text
 */
export function validateMessage(text: string): { valid: boolean; error?: string } {
  if (!text || !text.trim()) {
    return { valid: false, error: 'Le message ne peut pas être vide' };
  }
  
  if (text.length > 5000) {
    return { valid: false, error: 'Le message ne peut pas dépasser 5000 caractères' };
  }
  
  return { valid: true };
}

/**
 * Debounce function
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout> | null = null;
  
  return function executedFunction(...args: Parameters<T>) {
    const later = () => {
      timeout = null;
      func(...args);
    };
    
    if (timeout) {
      clearTimeout(timeout);
    }
    timeout = setTimeout(later, wait);
  };
}

/**
 * Throttle function
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean;
  
  return function executedFunction(...args: Parameters<T>) {
    if (!inThrottle) {
      func(...args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
}

/**
 * Deep clone object
 */
export function deepClone<T>(obj: T): T {
  return JSON.parse(JSON.stringify(obj));
}

/**
 * Check if object is empty
 */
export function isEmpty(obj: any): boolean {
  if (obj == null) return true;
  if (Array.isArray(obj)) return obj.length === 0;
  if (typeof obj === 'object') return Object.keys(obj).length === 0;
  return false;
}

/**
 * Merge objects
 */
export function mergeObjects<T extends Record<string, any>>(...objects: T[]): T {
  return Object.assign({}, ...objects);
}

/**
 * Get nested object value
 */
export function getNestedValue(obj: any, path: string): any {
  return path.split('.').reduce((current, key) => {
    return current && current[key] !== undefined ? current[key] : undefined;
  }, obj);
}

/**
 * Set nested object value
 */
export function setNestedValue(obj: any, path: string, value: any): void {
  const keys = path.split('.');
  const lastKey = keys.pop()!;
  const target = keys.reduce((current, key) => {
    if (!current[key]) {
      current[key] = {};
    }
    return current[key];
  }, obj);
  target[lastKey] = value;
}
