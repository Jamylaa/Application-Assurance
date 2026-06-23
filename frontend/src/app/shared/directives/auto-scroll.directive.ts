import { Directive, ElementRef, NgZone, AfterViewInit } from '@angular/core';
import { fromEvent } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

@Directive({
  selector: '[appAutoScroll]',
  standalone: true
})
export class AutoScrollDirective implements AfterViewInit {
  private isUserScrolling = false;
  private scrollTimeout: any;
  private lastScrollTop = 0;

  constructor(
    private elementRef: ElementRef,
    private ngZone: NgZone
  ) {}

  ngAfterViewInit(): void {
    this.setupScrollListener();
  }

  private setupScrollListener(): void {
    this.ngZone.runOutsideAngular(() => {
      fromEvent(this.elementRef.nativeElement, 'scroll')
        .pipe(debounceTime(50))
        .subscribe(() => {
          const element = this.elementRef.nativeElement;
          const scrollTop = element.scrollTop;
          const scrollHeight = element.scrollHeight;
          const clientHeight = element.clientHeight;
          const scrollBottom = scrollHeight - scrollTop - clientHeight;

          // Detect if user is scrolling up
          this.isUserScrolling = scrollTop < this.lastScrollTop;
          this.lastScrollTop = scrollTop;

          // Clear previous timeout
          if (this.scrollTimeout) {
            clearTimeout(this.scrollTimeout);
          }

          // Reset user scrolling flag after 1 second of inactivity
          this.scrollTimeout = setTimeout(() => {
            this.isUserScrolling = false;
          }, 1000);
        });
    });
  }

  /**
   * Scroll to bottom of the element
   */
  scrollToBottom(smooth: boolean = true): void {
    const element = this.elementRef.nativeElement;
    this.ngZone.runOutsideAngular(() => {
      if (smooth) {
        element.scrollTo({
          top: element.scrollHeight,
          behavior: 'smooth'
        });
      } else {
        element.scrollTop = element.scrollHeight;
      }
    });
  }

  /**
   * Scroll to top of the element
   */
  scrollToTop(smooth: boolean = true): void {
    const element = this.elementRef.nativeElement;
    this.ngZone.runOutsideAngular(() => {
      if (smooth) {
        element.scrollTo({
          top: 0,
          behavior: 'smooth'
        });
      } else {
        element.scrollTop = 0;
      }
    });
  }

  /**
   * Check if user is currently scrolling
   */
  isUserInteracting(): boolean {
    return this.isUserScrolling;
  }

  /**
   * Check if element is scrolled to bottom
   */
  isAtBottom(threshold: number = 50): boolean {
    const element = this.elementRef.nativeElement;
    const scrollHeight = element.scrollHeight;
    const scrollTop = element.scrollTop;
    const clientHeight = element.clientHeight;
    const scrollBottom = scrollHeight - scrollTop - clientHeight;
    return scrollBottom <= threshold;
  }

  /**
   * Scroll to a specific element
   */
  scrollToElement(targetElement: HTMLElement, smooth: boolean = true): void {
    const element = this.elementRef.nativeElement;
    const targetPosition = targetElement.offsetTop;
    
    this.ngZone.runOutsideAngular(() => {
      if (smooth) {
        element.scrollTo({
          top: targetPosition,
          behavior: 'smooth'
        });
      } else {
        element.scrollTop = targetPosition;
      }
    });
  }
}
