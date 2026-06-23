import { Directive, ElementRef, EventEmitter, Output, HostListener, OnDestroy } from '@angular/core';
import { fromEvent, Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';

export interface KeyboardShortcut {
  key: string;
  ctrl?: boolean;
  alt?: boolean;
  shift?: boolean;
  description?: string;
}

@Directive({
  selector: '[appKeyboardShortcut]',
  standalone: true
})
export class KeyboardShortcutDirective implements OnDestroy {
  @Output() shortcut = new EventEmitter<KeyboardShortcut>();
  
  private subscription: Subscription | null = null;

  constructor(private elementRef: ElementRef) {
    this.setupKeyboardListener();
  }

  private setupKeyboardListener(): void {
    this.subscription = fromEvent<KeyboardEvent>(document, 'keydown')
      .pipe(filter(() => this.isElementFocused()))
      .subscribe(event => this.handleKeyDown(event));
  }

  private handleKeyDown(event: KeyboardEvent): void {
    const shortcut: KeyboardShortcut = {
      key: event.key,
      ctrl: event.ctrlKey,
      alt: event.altKey,
      shift: event.shiftKey
    };

    this.shortcut.emit(shortcut);
  }

  private isElementFocused(): boolean {
    const activeElement = document.activeElement;
    const element = this.elementRef.nativeElement;
    
    // Check if the focused element is inside the directive's element
    return element.contains(activeElement);
  }

  @HostListener('window:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    // Common shortcuts
    if (event.ctrlKey || event.metaKey) {
      switch (event.key.toLowerCase()) {
        case 'k':
          event.preventDefault();
          this.shortcut.emit({ key: 'k', ctrl: true, description: 'Search' });
          break;
        case 'e':
          event.preventDefault();
          this.shortcut.emit({ key: 'e', ctrl: true, description: 'Export' });
          break;
        case 'n':
          event.preventDefault();
          this.shortcut.emit({ key: 'n', ctrl: true, description: 'New conversation' });
          break;
        case '/':
          event.preventDefault();
          this.shortcut.emit({ key: '/', ctrl: true, description: 'Focus input' });
          break;
      }
    }

    // Escape key
    if (event.key === 'Escape') {
      this.shortcut.emit({ key: 'Escape', description: 'Close/Cancel' });
    }

    // Enter key (without shift)
    if (event.key === 'Enter' && !event.shiftKey) {
      const activeElement = document.activeElement;
      if (activeElement && activeElement.tagName === 'TEXTAREA') {
        this.shortcut.emit({ key: 'Enter', description: 'Send message' });
      }
    }
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
}
