import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/theme.service';
import { ThemeSwitcherComponent } from './shared/theme-switcher/theme-switcher.component';
import { ToastComponent } from './shared/components/toast.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ThemeSwitcherComponent, ToastComponent],
  templateUrl: './app.component.html'
})
export class AppComponent {
  title = 'vermeg-assurance';

  constructor(public readonly themeService: ThemeService) {
    this.themeService.init();
  }
}