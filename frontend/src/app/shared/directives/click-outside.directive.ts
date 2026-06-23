import { Directive, ElementRef, EventEmitter, Output, NgZone, OnDestroy } from '@angular/core';
import { fromEvent, Subscription } from 'rxjs';

@Directive({
  selector: '[appClickOutside]',
  standalone: true
})
export class ClickOutsideDirective implements OnDestroy {
  @Output() clickOutside = new EventEmitter<void>();

  private subscription: Subscription | null = null;

  constructor(
    private elementRef: ElementRef,
    private ngZone: NgZone
  ) {
    this.setupClickListener();
  }

  private setupClickListener(): void {
    this.ngZone.runOutsideAngular(() => {
      this.subscription = fromEvent(document, 'click').subscribe((event: Event) => {
        this.handleClick(event as MouseEvent);
      });
    });
  }

  private handleClick(event: MouseEvent): void {
    const clickedInside = this.elementRef.nativeElement.contains(event.target);
    
    if (!clickedInside) {
      this.ngZone.run(() => {
        this.clickOutside.emit();
      });
    }
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
}
