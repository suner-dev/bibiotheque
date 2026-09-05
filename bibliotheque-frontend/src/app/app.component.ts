import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { routeAnimation } from './_animations/route.animation';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  animations: [routeAnimation]
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Library Management System';

  private sub = new Subscription();

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Scroll en haut de page à chaque navigation, synchronisé avec la transition
    this.sub.add(
      this.router.events
        .pipe(filter(event => event instanceof NavigationEnd))
        .subscribe(() => {
          // Petit délai pour laisser l'animation de sortie démarrer
          requestAnimationFrame(() => {
            window.scrollTo({ top: 0, behavior: 'smooth' });
          });
        })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  /** Retourne le data.animation de la route active pour déclencher la transition */
  getRouteAnimationData(outlet: RouterOutlet): string {
    return outlet && outlet.activatedRouteData
      ? outlet.activatedRouteData['animation']
      : '';
  }
}
