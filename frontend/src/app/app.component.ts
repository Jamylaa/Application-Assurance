import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/theme.service';
import { TranslationService } from './services/translation.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html'
})
export class AppComponent {
  title = 'vermeg-assurance';

  constructor(
    public readonly themeService: ThemeService,
    private readonly translationService: TranslationService
  ) {
    this.themeService.init();
    this.translationService.initializeLanguage();
  }
}