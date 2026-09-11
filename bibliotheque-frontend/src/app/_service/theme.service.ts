import { Injectable } from '@angular/core';

export type Theme = 'dark' | 'light';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {

  private readonly STORAGE_KEY = 'bibliotheque-theme';

  constructor() {
    this.init();
  }

  /** Initialise le thème au démarrage de l'app */
  private init(): void {
    const saved = this.getSaved();
    this.apply(saved);
  }

  /** Retourne le thème courant */
  get current(): Theme {
    return this.getSaved();
  }

  /** Vérifie si on est en mode sombre */
  get isDark(): boolean {
    return this.current === 'dark';
  }

  /** Toggle entre dark et light */
  toggle(): void {
    const next: Theme = this.isDark ? 'light' : 'dark';
    this.apply(next);
    this.save(next);
  }

  /** Applique le thème au DOM */
  private apply(theme: Theme): void {
    const root = document.documentElement;
    root.setAttribute('data-theme', theme);

    // Met à jour le background du body
    if (theme === 'light') {
      document.body.style.background = 'var(--bg-base)';
      document.body.style.color = 'var(--text-primary)';
    } else {
      document.body.style.background = 'var(--bg-base)';
      document.body.style.color = 'var(--text-primary)';
    }
  }

  /** Sauvegarde dans localStorage */
  private save(theme: Theme): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, theme);
    } catch {
      // localStorage peut être indisponible
    }
  }

  /** Lecture depuis localStorage (défaut: dark) */
  private getSaved(): Theme {
    try {
      const saved = localStorage.getItem(this.STORAGE_KEY);
      if (saved === 'light' || saved === 'dark') {
        return saved;
      }
    } catch {
      // localStorage peut être indisponible
    }
    return 'dark';
  }
}
