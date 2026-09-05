import {
  trigger,
  transition,
  style,
  animate,
  query,
  group,
  animateChild
} from '@angular/animations';

/**
 * Animation de transition entre pages.
 *
 * Chaque route porte un `data.animation` (ex: 'books', 'login'…).
 * L'animation calcule la direction de navigation (forward / backward)
 * en comparant les profondeurs des routes dans le tableau ORDER.
 */

const ORDER = [
  '',
  'login',
  'books',
  'create-book',
  'update-book',
  'book-details',
  'users',
  'register-user',
  'user-details',
  'update-user',
  'borrow-book',
  'return-book',
  'reservations',
  'create-reservation',
  'forbidden'
];

function depth(route: string): number {
  const idx = ORDER.indexOf(route);
  return idx === -1 ? 0 : idx;
}

/** Durée totale de la transition (ms) */
const DURATION = 400;

export const routeAnimation = trigger('routeAnimation', [
  transition('* <=> *', [
    /* — 1. Préparer les deux pages — */
    query(':enter, :leave', [
      style({
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%'
      })
    ], { optional: true }),

    /* — 2. Sortir l'ancienne page — */
    query(':leave', animateChild(), { optional: true }),

    group([
      query(':leave', [
        style({ opacity: 1, transform: 'translateY(0) scale(1)' }),
        animate(
          `${DURATION * 0.5}ms cubic-bezier(0.4, 0, 0.2, 1)`,
          style({
            opacity: 0,
            transform: 'translateY(-30px) scale(0.98)'
          })
        )
      ], { optional: true }),

      /* — 3. Entrer la nouvelle page — */
      query(':enter', [
        style({
          opacity: 0,
          transform: 'translateY(40px) scale(0.97)',
          filter: 'blur(4px)'
        }),
        animate(
          `${DURATION * 0.6}ms ${DURATION * 0.35}ms cubic-bezier(0.22, 1, 0.36, 1)`,
          style({
            opacity: 1,
            transform: 'translateY(0) scale(1)',
            filter: 'blur(0px)'
          })
        )
      ], { optional: true })
    ]),

    /* — 4. Animer les enfants (composants internes) — */
    query(':enter', animateChild(), { optional: true })
  ])
]);
