import { Injectable } from '@angular/core';
import { Observable, of, BehaviorSubject } from 'rxjs';
import { map, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import Fuse, { FuseResult, FuseResultMatch, IFuseOptions } from 'fuse.js';

export interface SearchOptions {
  keys?: string[];
  threshold?: number;
  includeScore?: boolean;
  limit?: number;
}

export interface SearchResult<T> {
  item: T;
  score?: number;
  matches?: readonly FuseResultMatch[];
}

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private searchHistory: string[] = [];
  private maxHistorySize = 10;
  private searchHistorySubject = new BehaviorSubject<string[]>([]);

  readonly searchHistory$ = this.searchHistorySubject.asObservable();

  /**
   * Search in array using Fuse.js for fuzzy search
   */
  fuzzySearch<T>(data: T[], query: string, options: SearchOptions = {}): Observable<SearchResult<T>[]> {
    if (!query.trim()) {
      return of([]);
    }

    const fuseOptions: IFuseOptions<T> = {
      keys: options.keys || ['title', 'text', 'name'],
      threshold: options.threshold ?? 0.3,
      includeScore: options.includeScore ?? true,
      includeMatches: true,
      minMatchCharLength: 2
    };

    const fuse = new Fuse(data, fuseOptions);
    const results = fuse.search(query, { limit: options.limit });

    return of(results.map(result => ({
      item: result.item,
      score: result.score,
      matches: result.matches
    })));
  }

  /**
   * Simple text search (case-insensitive)
   */
  textSearch<T>(data: T[], query: string, searchFields: string[] = []): Observable<T[]> {
    if (!query.trim()) {
      return of(data);
    }

    const lowerQuery = query.toLowerCase();
    const results = data.filter(item => {
      if (searchFields.length === 0) {
        return this.searchInObject(item, lowerQuery);
      }
      return searchFields.some(field => {
        const value = this.getNestedValue(item, field);
        return value && String(value).toLowerCase().includes(lowerQuery);
      });
    });

    return of(results);
  }

  /**
   * Search with filters
   */
  filteredSearch<T>(
    data: T[],
    query: string,
    filters: { [key: string]: any },
    searchFields: string[] = []
  ): Observable<T[]> {
    let results = data;

    // Apply text search
    if (query.trim()) {
      results = this.textSearchSync(results, query, searchFields);
    }

    // Apply filters
    results = results.filter(item => {
      return Object.entries(filters).every(([key, value]) => {
        if (value === null || value === undefined || value === '') {
          return true;
        }
        const itemValue = this.getNestedValue(item, key);
        if (Array.isArray(value)) {
          return value.includes(itemValue);
        }
        return itemValue === value;
      });
    });

    return of(results);
  }

  /**
   * Add query to search history
   */
  addToHistory(query: string): void {
    const trimmedQuery = query.trim();
    if (!trimmedQuery) return;

    // Remove if already exists
    this.searchHistory = this.searchHistory.filter(q => q !== trimmedQuery);

    // Add to beginning
    this.searchHistory.unshift(trimmedQuery);

    // Limit size
    this.searchHistory = this.searchHistory.slice(0, this.maxHistorySize);

    this.searchHistorySubject.next(this.searchHistory);
  }

  /**
   * Get search history
   */
  getSearchHistory(): Observable<string[]> {
    return this.searchHistory$;
  }

  /**
   * Clear search history
   */
  clearHistory(): void {
    this.searchHistory = [];
    this.searchHistorySubject.next(this.searchHistory);
  }

  /**
   * Remove specific query from history
   */
  removeFromHistory(query: string): void {
    this.searchHistory = this.searchHistory.filter(q => q !== query);
    this.searchHistorySubject.next(this.searchHistory);
  }

  /**
   * Search in messages with context
   */
  searchMessagesWithContext(
    messages: any[],
    query: string,
    contextSize: number = 2
  ): Observable<Array<{ message: any; context: any[] }>> {
    if (!query.trim()) {
      return of([]);
    }

    const lowerQuery = query.toLowerCase();
    const matches: Array<{ message: any; context: any[] }> = [];

    messages.forEach((msg, index) => {
      if (msg.text && msg.text.toLowerCase().includes(lowerQuery)) {
        const start = Math.max(0, index - contextSize);
        const end = Math.min(messages.length, index + contextSize + 1);
        const context = messages.slice(start, end);
        matches.push({ message: msg, context });
      }
    });

    return of(matches);
  }

  /**
   * Highlight search terms in text
   */
  highlightText(text: string, query: string): string {
    if (!query.trim()) return text;

    const regex = new RegExp(`(${this.escapeRegex(query)})`, 'gi');
    return text.replace(regex, '<mark>$1</mark>');
  }

  /**
   * Get search suggestions based on partial input
   */
  getSuggestions(partial: string, data: string[]): Observable<string[]> {
    if (!partial.trim()) {
      return of([]);
    }

    const lowerPartial = partial.toLowerCase();
    const suggestions = data
      .filter(item => item.toLowerCase().includes(lowerPartial))
      .slice(0, 5);

    return of(suggestions);
  }

  /**
   * Debounced search observable creator
   */
  createDebouncedSearch<T>(
    searchFn: (query: string) => Observable<T[]>,
    debounceMs: number = 300
  ): (source$: Observable<string>) => Observable<T[]> {
    return (source$) =>
      source$.pipe(
        debounceTime(debounceMs),
        distinctUntilChanged(),
        map(query => query.trim()),
        switchMap(query => {
          if (!query) return of([]);
          return searchFn(query);
        })
      );
  }

  /**
   * Search recursively in object
   */
  private searchInObject(obj: any, query: string): boolean {
    if (!obj) return false;

    if (typeof obj === 'string') {
      return obj.toLowerCase().includes(query);
    }

    if (typeof obj === 'number' || typeof obj === 'boolean') {
      return String(obj).toLowerCase().includes(query);
    }

    if (Array.isArray(obj)) {
      return obj.some(item => this.searchInObject(item, query));
    }

    if (typeof obj === 'object') {
      return Object.values(obj).some(value => this.searchInObject(value, query));
    }

    return false;
  }

  /**
   * Get nested object value by path
   */
  private getNestedValue(obj: any, path: string): any {
    return path.split('.').reduce((current, key) => {
      return current && current[key] !== undefined ? current[key] : undefined;
    }, obj);
  }

  /**
   * Escape special regex characters
   */
  private escapeRegex(string: string): string {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  /**
   * Sync version of text search
   */
  private textSearchSync<T>(data: T[], query: string, searchFields: string[] = []): T[] {
    const lowerQuery = query.toLowerCase();
    return data.filter(item => {
      if (searchFields.length === 0) {
        return this.searchInObject(item, lowerQuery);
      }
      return searchFields.some(field => {
        const value = this.getNestedValue(item, field);
        return value && String(value).toLowerCase().includes(lowerQuery);
      });
    });
  }
}
