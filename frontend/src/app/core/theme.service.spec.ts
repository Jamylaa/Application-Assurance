import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/common';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;
  let document: Document;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
    document = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('démarre en thème clair par défaut', () => {
    expect(service.currentTheme).toBe('light');
  });

  it('setTheme applique le thème au <html> et le persiste', () => {
    service.setTheme('dark');

    expect(service.currentTheme).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem('theme')).toBe('dark');
  });

  it('toggle bascule clair <-> sombre', () => {
    service.setTheme('light');

    service.toggle();
    expect(service.currentTheme).toBe('dark');

    service.toggle();
    expect(service.currentTheme).toBe('light');
  });

  it('init lit le thème précédemment stocké', () => {
    localStorage.setItem('theme', 'dark');

    service.init();

    expect(service.currentTheme).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });
});
