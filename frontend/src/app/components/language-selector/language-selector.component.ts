import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslationService } from '../../services/translation.service';

@Component({
  selector: 'app-language-selector',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="language-selector">
      <select (change)="changeLanguage($event)" [value]="currentLang">
        <option value="fr">{{ translate('language.french') }}</option>
        <option value="en">{{ translate('language.english') }}</option>
      </select>
    </div>
  `,
  styles: [`
    .language-selector select {
      padding: 8px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      background-color: white;
      cursor: pointer;
      font-size: 14px;
    }
    .language-selector select:hover {
      border-color: #007bff;
    }
  `]
})
export class LanguageSelectorComponent {
  currentLang: string;

  constructor(private translationService: TranslationService) {
    this.currentLang = this.translationService.getCurrentLanguage();
    this.translationService.currentLang$.subscribe(lang => {
      this.currentLang = lang;
    });
  }

  changeLanguage(event: Event) {
    const selectElement = event.target as HTMLSelectElement;
    this.translationService.setLanguage(selectElement.value);
  }

  translate(key: string): string {
    return this.translationService.translate(key);
  }
}
