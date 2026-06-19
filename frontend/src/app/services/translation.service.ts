import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TranslationService {
  private currentLang = new BehaviorSubject<string>('fr');
  currentLang$ = this.currentLang.asObservable();

  private translations: { [key: string]: any } = {};
  private translationsLoaded = new BehaviorSubject<boolean>(false);
  translationsLoaded$ = this.translationsLoaded.asObservable();

  constructor(private http: HttpClient) {
    this.loadTranslations(this.currentLang.value);
  }

  setLanguage(lang: string) {
    this.currentLang.next(lang);
    this.loadTranslations(lang);
    localStorage.setItem('preferredLanguage', lang);
  }

  getCurrentLanguage(): string {
    return this.currentLang.value;
  }

  private loadTranslations(lang: string) {
    this.translationsLoaded.next(false);
    this.http.get(`./assets/i18n/${lang}.json`).subscribe(
      (data: any) => {
        this.translations = data;
        this.translationsLoaded.next(true);
      },
      (error) => {
        console.error('Error loading translations:', error);
        this.translationsLoaded.next(true);
      }
    );
  }

  translate(key: string): string {
    const keys = key.split('.');
    let value: any = this.translations;

    for (const k of keys) {
      if (value && value[k]) {
        value = value[k];
      } else {
        return key;
      }
    }
    return typeof value === 'string' ? value : key;
  }

  instant(key: string): string {
    return this.translate(key);
  }

  initializeLanguage() {
    const savedLang = localStorage.getItem('preferredLanguage');
    const browserLang = navigator.language.startsWith('fr') ? 'fr' : 'en';
    const initialLang = savedLang || browserLang;
    this.setLanguage(initialLang);
  }
}
