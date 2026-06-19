import { Component, inject } from '@angular/core';
import { ThemeService } from '../../core/theme.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-theme-switcher',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './theme-switcher.component.html',
  styleUrls: ['./theme-switcher.component.css']
})
export class ThemeSwitcherComponent {
  private themeService = inject(ThemeService);

  currentTheme = this.themeService.currentTheme;
  isDarkMode = this.currentTheme === 'dark';

  toggleTheme(): void {
    this.themeService.toggle();
    this.currentTheme = this.themeService.currentTheme;
    this.isDarkMode = this.currentTheme === 'dark';
  }

  setLightMode(): void {
    this.themeService.setTheme('light');
    this.currentTheme = 'light';
    this.isDarkMode = false;
  }

  setDarkMode(): void {
    this.themeService.setTheme('dark');
    this.currentTheme = 'dark';
    this.isDarkMode = true;
  }
}
