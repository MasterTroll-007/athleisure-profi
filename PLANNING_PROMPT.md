# AI Prompt: Rezervační systém pro fitness trenérku

## MOBILNÍ APLIKACE S ATHLEISURE MINIMAL DESIGNEM

---

## HLAVNÍ PROMPT (Celý projekt)

```
Vytvoř kompletní rezervační systém pro osobní fitness trenérku. Systém bude primárně mobilní aplikace (mobile-first) s podporou desktopu. React frontend a Kotlin backend s PostgreSQL databází, vše běžící v Dockeru.

## TECHNICKÝ STACK

**Frontend:**
- React 18+ s Vite
- Tailwind CSS s custom design systémem (athleisure minimal)
- React Router v6
- TanStack Query pro API volání
- react-i18next pro lokalizaci (čeština + angličtina)
- React Hook Form + Zod pro validaci
- Zustand pro state management
- FullCalendar pro kalendářové zobrazení
- Framer Motion pro animace

**Backend:**
- Kotlin s Ktor frameworkem
- PostgreSQL databáze
- Exposed ORM
- JWT autentizace
- GoPay SDK pro platby

**Infrastruktura:**
- Docker + Docker Compose
- Nginx jako reverse proxy

---

## DESIGN SYSTÉM - ATHLEISURE MINIMAL

### Filosofie designu
- **Feeling:** Energický, sebevědomý, ale ne agresivní - "studio/pilates/strength" vibe
- **Základ:** Čistý, přehledný UI s hodně whitespace, jasná typografie, jednoduché zaoblené bloky
- **Premium akcenty:** Jemné, sofistikované detaily bez přehánění

### Barevná paleta

```javascript
// tailwind.config.js - colors
colors: {
  // Primární - pro CTA (energická ale ne neonová)
  primary: {
    50: '#fdf4f3',
    100: '#fce8e6',
    200: '#f9d5d2',
    300: '#f4b5af',
    400: '#ec8b82',
    500: '#e05d52',  // Hlavní CTA barva - "terracotta rose"
    600: '#cc4339',
    700: '#ab362e',
    800: '#8d302a',
    900: '#752d28',
  },
  // Neutrální - warm gray
  neutral: {
    50: '#fafaf9',
    100: '#f5f5f4',
    200: '#e7e5e4',
    300: '#d6d3d1',
    400: '#a8a29e',
    500: '#78716c',
    600: '#57534e',
    700: '#44403c',
    800: '#292524',
    900: '#1c1917',
    950: '#0c0a09',
  },
  // Status barvy pro kalendář
  status: {
    available: '#e7f5e8',      // Jemná zelená - volné sloty
    availableBorder: '#86c789',
    booked: '#e8e0f5',         // Jemná fialová - rezervováno
    bookedBorder: '#a78bcc',
    pending: '#fff4e6',        // Jemná oranžová - čeká na potvrzení
    pendingBorder: '#f5a623',
    cancelled: '#f5e7e7',      // Jemná červená - zrušeno
    cancelledBorder: '#d6a5a5',
    mine: '#e0f0f5',           // Jemná modrá - moje rezervace
    mineBorder: '#7bb8cc',
  },
  // Dark mode
  dark: {
    bg: '#0c0a09',
    surface: '#1c1917',
    surfaceHover: '#292524',
    border: '#44403c',
  }
}
```

### Typografie

```javascript
// tailwind.config.js - fonts
fontFamily: {
  // Nadpisy - sebevědomé, silné
  heading: ['Outfit', 'sans-serif'],
  // UI texty - čitelné, přátelské
  body: ['Inter', 'sans-serif'],
  // Čísla v kalendáři - monospace pro alignment
  mono: ['JetBrains Mono', 'monospace'],
}

// Velikosti pro mobile-first
fontSize: {
  'xs': ['0.75rem', { lineHeight: '1rem' }],
  'sm': ['0.875rem', { lineHeight: '1.25rem' }],
  'base': ['1rem', { lineHeight: '1.5rem' }],
  'lg': ['1.125rem', { lineHeight: '1.75rem' }],
  'xl': ['1.25rem', { lineHeight: '1.75rem' }],
  '2xl': ['1.5rem', { lineHeight: '2rem' }],
  '3xl': ['1.875rem', { lineHeight: '2.25rem' }],
  'display': ['2.25rem', { lineHeight: '2.5rem', fontWeight: '700' }],
}
```

### Komponenty - Tvarosloví

```css
/* Zaoblení - větší, přátelské */
--radius-sm: 8px;
--radius-md: 12px;
--radius-lg: 16px;
--radius-xl: 24px;
--radius-full: 9999px;

/* Stíny - jemné, ne tvrdé */
--shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.03);
--shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.05), 0 2px 4px -2px rgb(0 0 0 / 0.05);
--shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.05), 0 4px 6px -4px rgb(0 0 0 / 0.05);
--shadow-glow: 0 0 20px rgb(224 93 82 / 0.15);

/* Glassmorphism - POUZE pro specifické elementy */
.glass {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.glass-dark {
  background: rgba(28, 25, 23, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
```

### Kde použít Glassmorphism (a kde NE)

**POUŽÍT:**
- Fullscreen hamburger menu overlay
- Modal okna (rezervace, potvrzení, platba)
- Toast notifikace
- Sticky header při scrollu
- Bottom navigation bar na mobilu

**NEPOUŽÍVAT:**
- Kalendářový grid (potřebuje kontrast!)
- Karty se sloty (musí být čitelné)
- Formulářové inputy
- Hlavní obsahová plocha

---

## MOBILE-FIRST UI KOMPONENTY

### 1. Fullscreen Hamburger Menu

```tsx
// components/MobileMenu.tsx
interface MobileMenuProps {
  isOpen: boolean;
  onClose: () => void;
}

/*
DESIGN SPECIFIKACE:
- Fullscreen overlay s glass efektem
- Animace: slide-in from right (300ms ease-out)
- Menu položky: velké, snadno kliknutelné (min 48px výška)
- Aktivní položka: primární barva s jemným glow
- Přepínač Dark/Light mode dole
- Jazyk přepínač (CZ/EN)
- Logout tlačítko
- Zavírací X v pravém horním rohu (velký, 44x44px)
*/

const menuItems = [
  { icon: 'home', label: 'nav.home', path: '/' },
  { icon: 'calendar', label: 'nav.reservations', path: '/reservations' },
  { icon: 'file-text', label: 'nav.plans', path: '/plans' },
  { icon: 'user', label: 'nav.profile', path: '/profile' },
  { icon: 'credit-card', label: 'nav.credits', path: '/credits' },
];

// Admin položky (pokud role === 'admin')
const adminItems = [
  { icon: 'layout-dashboard', label: 'nav.admin', path: '/admin' },
];
```

### 2. Bottom Navigation (Mobile)

```tsx
// components/BottomNav.tsx
/*
DESIGN SPECIFIKACE:
- Fixed bottom, glass efekt
- 4-5 hlavních položek s ikonami
- Aktivní položka: ikona vyplněná + primární barva
- Safe area padding pro notch/home indicator
- Výška: 64px + safe area
*/

const navItems = [
  { icon: 'home', label: 'Domů', path: '/' },
  { icon: 'calendar-plus', label: 'Rezervace', path: '/reservations/new' },
  { icon: 'calendar', label: 'Moje', path: '/reservations' },
  { icon: 'user', label: 'Profil', path: '/profile' },
];
```

### 3. Header s Dark/Light Toggle

```tsx
// components/Header.tsx
/*
DESIGN SPECIFIKACE:
- Sticky top, glass efekt při scrollu
- Levá strana: Logo/název
- Pravá strana: Dark/Light toggle + Hamburger menu
- Dark/Light toggle: animovaná ikona (sun/moon)
- Výška: 56px
*/
```

### 4. Dark/Light Mode Toggle

```tsx
// components/ThemeToggle.tsx
/*
DESIGN SPECIFIKACE:
- Plynulá animace přechodu (sun → moon)
- Subtle glow efekt na hover
- Velikost: 40x40px touch target
- Uložení preference do localStorage + system preference fallback
*/
```

---

## FULLCALENDAR INTEGRACE

### Konfigurace

```tsx
// components/Calendar/BookingCalendar.tsx
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';

/*
DESIGN SPECIFIKACE pro FullCalendar:
- ŽÁDNÝ glassmorphism na kalendáři!
- Čisté, kontrastní pozadí (bílá/tmavá podle theme)
- Sloty s jasnou barevnou indikací statusu
- Velké touch targety pro mobilní zařízení
- Custom toolbar pro mobile (kompaktní)
*/

const calendarConfig = {
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'timeGridWeek', // Desktop
  // Mobile: přepnout na 'timeGridDay' nebo 'listWeek'

  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay'
  },

  // Mobile toolbar (zjednodušený)
  mobileToolbar: {
    left: 'prev,next',
    center: 'title',
    right: 'today'
  },

  slotMinTime: '06:00:00',
  slotMaxTime: '21:00:00',
  slotDuration: '00:30:00',

  // Touch friendly
  selectable: true,
  selectMirror: true,
  dayMaxEvents: true,

  // Locale
  locale: 'cs', // nebo 'en' podle i18n
  firstDay: 1, // Pondělí

  // Custom event rendering
  eventContent: renderEventContent,
  eventClassNames: getEventClassNames,
};

// Custom renderování eventů
function renderEventContent(eventInfo) {
  return (
    <div className="fc-event-custom">
      <span className="fc-event-time font-mono text-sm font-semibold">
        {eventInfo.timeText}
      </span>
      <span className="fc-event-title text-sm">
        {eventInfo.event.title}
      </span>
      {eventInfo.event.extendedProps.clientName && (
        <span className="fc-event-client text-xs opacity-80">
          {eventInfo.event.extendedProps.clientName}
        </span>
      )}
    </div>
  );
}

// CSS třídy podle statusu
function getEventClassNames(eventInfo) {
  const status = eventInfo.event.extendedProps.status;
  return [`fc-event-${status}`];
}
```

### FullCalendar Custom Styles

```css
/* styles/fullcalendar-custom.css */

/* Reset FullCalendar default styles pro náš design */
.fc {
  --fc-border-color: theme('colors.neutral.200');
  --fc-page-bg-color: transparent;
  --fc-neutral-bg-color: theme('colors.neutral.50');
  --fc-today-bg-color: theme('colors.primary.50');

  font-family: theme('fontFamily.body');
}

/* Dark mode */
.dark .fc {
  --fc-border-color: theme('colors.dark.border');
  --fc-page-bg-color: transparent;
  --fc-neutral-bg-color: theme('colors.dark.surface');
  --fc-today-bg-color: rgba(224, 93, 82, 0.1);
}

/* Header toolbar - mobile friendly */
.fc .fc-toolbar {
  flex-wrap: wrap;
  gap: 0.5rem;
}

@media (max-width: 640px) {
  .fc .fc-toolbar {
    flex-direction: column;
  }

  .fc .fc-toolbar-chunk {
    display: flex;
    justify-content: center;
  }
}

/* Tlačítka */
.fc .fc-button {
  @apply rounded-lg px-3 py-2 text-sm font-medium;
  @apply bg-neutral-100 text-neutral-700 border-none;
  @apply hover:bg-neutral-200 transition-colors;
}

.fc .fc-button-active {
  @apply bg-primary-500 text-white;
  @apply hover:bg-primary-600;
}

.dark .fc .fc-button {
  @apply bg-dark-surface text-neutral-200;
  @apply hover:bg-dark-surfaceHover;
}

/* Event styly podle statusu */
.fc-event-available {
  @apply bg-status-available border-l-4 border-status-availableBorder;
  @apply text-neutral-700;
}

.fc-event-booked {
  @apply bg-status-booked border-l-4 border-status-bookedBorder;
  @apply text-neutral-700;
}

.fc-event-pending {
  @apply bg-status-pending border-l-4 border-status-pendingBorder;
  @apply text-neutral-700;
}

.fc-event-cancelled {
  @apply bg-status-cancelled border-l-4 border-status-cancelledBorder;
  @apply text-neutral-500 line-through;
}

.fc-event-mine {
  @apply bg-status-mine border-l-4 border-status-mineBorder;
  @apply text-neutral-700;
}

/* Custom event content */
.fc-event-custom {
  @apply flex flex-col p-1;
}

.fc-event-time {
  @apply font-mono text-xs font-bold;
}

.fc-event-title {
  @apply text-sm font-medium truncate;
}

.fc-event-client {
  @apply text-xs opacity-70;
}

/* Touch friendly time slots */
@media (max-width: 640px) {
  .fc .fc-timegrid-slot {
    height: 3rem; /* Větší pro touch */
  }

  .fc-event {
    min-height: 2.5rem;
  }
}
```

---

## HLAVNÍ FUNKCE

### 1. Autentizace
- Registrace a přihlášení klientek
- JWT tokeny (access + refresh)
- Role: "client" a "admin"
- Hashování hesel (bcrypt)
- Biometrické přihlášení (volitelně pro PWA)

### 2. Systém rezervací s eliminací prostojů

KRITICKÁ LOGIKA - implementuj přesně takto:

1. Admin vytváří "bloky dostupnosti" - např. "Pondělí-Středa 14:00-18:00 bloky po 60min s 30min pauzou po dvou blocích"
2. Každý blok má definovanou délku tréninku (např. 60 minut)
3. PRVNÍ klientka v daném dni si může vybrat JAKÝKOLIV čas v rámci bloku
4. DALŠÍ klientky si MUSÍ vybrat čas "přilepený" k již existující rezervaci:
   - Těsně PŘED existující rezervací (nový konec = existující začátek)
   - Nebo těsně PO existující rezervaci (nový začátek = existující konec)
5. Tím vzniká souvislý blok bez mezer mezi tréninky

Příklad pro blok Po 14:00-18:00 (60min tréninky):
- Klientka A rezervuje 15:00-16:00
- Klientka B může: 14:00-15:00 NEBO 16:00-17:00
- A tak dále...

Uživatel může zrušit rezervaci nejpozději 24 hodin před daným termínem.

### 3. Kreditový systém

```typescript
// Ceník (definovaný adminem)
interface PricingItem {
  id: string;
  name_cs: string;
  name_en: string;
  credits: number;  // Kolik kreditů stojí
  description_cs?: string;
  description_en?: string;
  isActive: boolean;
}

// Příklad ceníku:
// - Jeden trénink: 1 kredit
// - 10 tréninků: 10 kreditů (se slevou při nákupu)
// - Individuální tréninkový plán: 5 kreditů
// - Kompletní fitness plán: 8 kreditů
// - Trénink student: 1 kredit (nižší cena za kredit)

// Kreditové balíčky k nákupu
interface CreditPackage {
  id: string;
  credits: number;
  price_czk: number;
  bonus_credits?: number;  // Bonus navíc
  name_cs: string;
  name_en: string;
}

// Příklad balíčků:
// - 1 kredit: 500 Kč
// - 5 kreditů: 2 250 Kč (ušetříte 250 Kč)
// - 10 kreditů: 4 000 Kč (ušetříte 1 000 Kč)
// - 20 kreditů: 7 000 Kč (ušetříte 3 000 Kč)
```

### 4. Prodej cvičebních plánů
- Admin nahrává PDF soubory (50+ stran)
- Každý plán má název, popis, cenu v kreditech, náhledový obrázek
- Klientka zakoupí za kredity
- Po zaplacení má přístup ke stažení v klientské zóně
- PDF servíruj přes API s autentizací (ne public URL)

### 5. GoPay integrace
- GoPay platební brána pro dobíjení kreditů
- Webhook pro potvrzení platby
- Ceny v CZK
- Flow: Klientka → vybere balíček kreditů → GoPay platba → kredity připsány

### 6. Klientská zóna (Mobile-first UI)

**Dashboard (Home):**
- Zůstatek kreditů (velký, výrazný)
- Další trénink (karta s odpočtem)
- Rychlé akce: Nová rezervace, Koupit kredity
- Poslední aktivita

**Moje rezervace:**
- Seznam s možností zrušení
- Vizuální rozdělení: nadcházející / minulé
- Pull-to-refresh

**Nová rezervace:**
- Kalendář s FullCalendar (mobile view)
- Výběr dostupného slotu
- Potvrzovací modal (glass efekt)

**Moje plány:**
- Zakoupené plány ke stažení
- Karty s náhledem

**Profil:**
- Úprava údajů
- Změna hesla
- Výběr jazyka (CZ/EN)
- Dark/Light mode
- Přehled kreditů + historie transakcí

**Dobít kredity:**
- Balíčky kreditů jako karty
- Výběr → GoPay platba
- Potvrzení

### 7. Admin panel

**Dashboard:**
- Dnešní tréninky (seznam)
- Týdenní statistiky
- Příjmy za období
- Rychlé akce

**Kalendář (FullCalendar):**
- Týdenní/měsíční pohled
- Všechny rezervace barevně podle statusu
- Klik = detail/editace

**Bloky dostupnosti:**
- CRUD pro časové bloky
- Vizuální náhled v kalendáři

**Klientky:**
- Seznam s vyhledáváním
- Detail: info, historie, poznámky, kredity
- Přidání/odebrání kreditů manuálně

**Cvičební plány:**
- CRUD
- Upload PDF a náhledu

**Ceník:**
- Správa položek ceníku
- Správa kreditových balíčků

**Platby:**
- Přehled transakcí
- Filtry podle data, klientky, typu

---

## DATABÁZOVÉ SCHÉMA

```sql
-- Uživatelé
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'client',
    credits INTEGER DEFAULT 0,
    locale VARCHAR(5) DEFAULT 'cs',
    theme VARCHAR(10) DEFAULT 'system', -- 'light', 'dark', 'system'
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Bloky dostupnosti
CREATE TABLE availability_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100),
    days_of_week INT[] NOT NULL, -- 1=Po, 2=Út, ... 7=Ne
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INT DEFAULT 60,
    break_after_slots INT, -- Pauza po X blocích
    break_duration_minutes INT, -- Délka pauzy
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Rezervace
CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    block_id UUID REFERENCES availability_blocks(id),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'confirmed', -- confirmed, cancelled, completed
    credits_used INTEGER DEFAULT 1,
    pricing_item_id UUID REFERENCES pricing_items(id),
    created_at TIMESTAMP DEFAULT NOW(),
    cancelled_at TIMESTAMP
);

-- Ceník položek
CREATE TABLE pricing_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_cs VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    description_cs TEXT,
    description_en TEXT,
    credits INTEGER NOT NULL, -- Kolik kreditů stojí
    is_active BOOLEAN DEFAULT true,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Kreditové balíčky
CREATE TABLE credit_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_cs VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    credits INTEGER NOT NULL,
    bonus_credits INTEGER DEFAULT 0,
    price_czk DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Transakce kreditů
CREATE TABLE credit_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    amount INTEGER NOT NULL, -- + pro připsání, - pro odečtení
    type VARCHAR(30) NOT NULL, -- 'purchase', 'reservation', 'plan_purchase', 'admin_adjustment', 'refund'
    reference_id UUID, -- ID rezervace, plánu, nebo balíčku
    gopay_payment_id VARCHAR(255),
    note TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Cvičební plány
CREATE TABLE training_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_cs VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    description_cs TEXT,
    description_en TEXT,
    credits INTEGER NOT NULL, -- Cena v kreditech
    file_path VARCHAR(500),
    preview_image VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Zakoupené plány
CREATE TABLE purchased_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    plan_id UUID REFERENCES training_plans(id),
    credits_used INTEGER NOT NULL,
    purchased_at TIMESTAMP DEFAULT NOW()
);

-- Poznámky ke klientkám
CREATE TABLE client_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    admin_id UUID REFERENCES users(id),
    note TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- GoPay platby
CREATE TABLE gopay_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    gopay_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'CZK',
    state VARCHAR(30) NOT NULL, -- CREATED, PAYMENT_METHOD_CHOSEN, PAID, AUTHORIZED, CANCELED, TIMEOUTED, REFUNDED
    credit_package_id UUID REFERENCES credit_packages(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

---

## STRUKTURA PROJEKTU

```
project/
├── docker-compose.yml
├── .env.example
├── README.md
│
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── postcss.config.js
│   ├── index.html
│   │
│   ├── public/
│   │   ├── manifest.json      # PWA manifest
│   │   ├── sw.js              # Service Worker
│   │   └── icons/
│   │
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── index.css          # Tailwind + custom styles
│       │
│       ├── components/
│       │   ├── ui/            # Základní UI komponenty
│       │   │   ├── Button.tsx
│       │   │   ├── Input.tsx
│       │   │   ├── Card.tsx
│       │   │   ├── Modal.tsx          # Glass efekt
│       │   │   ├── Toast.tsx          # Glass efekt
│       │   │   ├── Badge.tsx
│       │   │   ├── Spinner.tsx
│       │   │   └── index.ts
│       │   │
│       │   ├── layout/
│       │   │   ├── Header.tsx         # Sticky, glass on scroll
│       │   │   ├── BottomNav.tsx      # Mobile only, glass
│       │   │   ├── MobileMenu.tsx     # Fullscreen, glass
│       │   │   ├── ThemeToggle.tsx    # Dark/Light switch
│       │   │   ├── LanguageSwitch.tsx
│       │   │   └── Layout.tsx
│       │   │
│       │   ├── calendar/
│       │   │   ├── BookingCalendar.tsx    # FullCalendar wrapper
│       │   │   ├── SlotSelector.tsx       # Výběr slotu
│       │   │   ├── ReservationModal.tsx   # Potvrzení
│       │   │   └── CalendarStyles.css     # Custom FullCalendar styles
│       │   │
│       │   ├── credits/
│       │   │   ├── CreditBalance.tsx
│       │   │   ├── CreditPackageCard.tsx
│       │   │   └── TransactionHistory.tsx
│       │   │
│       │   └── admin/
│       │       ├── AdminLayout.tsx
│       │       ├── StatsCard.tsx
│       │       └── DataTable.tsx
│       │
│       ├── pages/
│       │   ├── Home.tsx
│       │   ├── Login.tsx
│       │   ├── Register.tsx
│       │   │
│       │   ├── reservations/
│       │   │   ├── NewReservation.tsx
│       │   │   └── MyReservations.tsx
│       │   │
│       │   ├── plans/
│       │   │   ├── PlansList.tsx
│       │   │   └── MyPlans.tsx
│       │   │
│       │   ├── profile/
│       │   │   ├── Profile.tsx
│       │   │   └── Settings.tsx
│       │   │
│       │   ├── credits/
│       │   │   └── BuyCredits.tsx
│       │   │
│       │   └── admin/
│       │       ├── Dashboard.tsx
│       │       ├── Calendar.tsx
│       │       ├── AvailabilityBlocks.tsx
│       │       ├── Clients.tsx
│       │       ├── ClientDetail.tsx
│       │       ├── TrainingPlans.tsx
│       │       ├── Pricing.tsx
│       │       └── Payments.tsx
│       │
│       ├── hooks/
│       │   ├── useAuth.ts
│       │   ├── useTheme.ts
│       │   ├── useCredits.ts
│       │   ├── useReservations.ts
│       │   └── useMobile.ts       # Detekce mobile/desktop
│       │
│       ├── services/
│       │   ├── api.ts             # Axios instance
│       │   ├── auth.ts
│       │   ├── reservations.ts
│       │   ├── credits.ts
│       │   ├── plans.ts
│       │   └── admin.ts
│       │
│       ├── stores/
│       │   ├── authStore.ts       # Zustand
│       │   └── themeStore.ts
│       │
│       ├── i18n/
│       │   ├── index.ts
│       │   ├── cs.json
│       │   └── en.json
│       │
│       ├── types/
│       │   ├── api.ts
│       │   ├── user.ts
│       │   ├── reservation.ts
│       │   └── credits.ts
│       │
│       └── utils/
│           ├── formatters.ts
│           ├── validators.ts
│           └── constants.ts
│
├── backend/
│   ├── Dockerfile
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   │
│   └── src/main/kotlin/
│       └── com/fitness/
│           ├── Application.kt
│           │
│           ├── config/
│           │   ├── DatabaseConfig.kt
│           │   ├── JwtConfig.kt
│           │   └── GopayConfig.kt
│           │
│           ├── models/
│           │   ├── User.kt
│           │   ├── AvailabilityBlock.kt
│           │   ├── Reservation.kt
│           │   ├── PricingItem.kt
│           │   ├── CreditPackage.kt
│           │   ├── CreditTransaction.kt
│           │   ├── TrainingPlan.kt
│           │   └── GopayPayment.kt
│           │
│           ├── repositories/
│           │   ├── UserRepository.kt
│           │   ├── ReservationRepository.kt
│           │   ├── CreditRepository.kt
│           │   └── PlanRepository.kt
│           │
│           ├── services/
│           │   ├── AuthService.kt
│           │   ├── AvailabilityService.kt
│           │   ├── ReservationService.kt
│           │   ├── CreditService.kt
│           │   ├── PlanService.kt
│           │   └── GopayService.kt
│           │
│           ├── routes/
│           │   ├── AuthRoutes.kt
│           │   ├── ReservationRoutes.kt
│           │   ├── CreditRoutes.kt
│           │   ├── PlanRoutes.kt
│           │   ├── AdminRoutes.kt
│           │   └── GopayWebhook.kt
│           │
│           └── plugins/
│               ├── Authentication.kt
│               ├── Routing.kt
│               ├── Serialization.kt
│               ├── CORS.kt
│               └── StatusPages.kt
│
├── nginx/
│   ├── nginx.conf
│   └── ssl/                   # Pro produkci
│
└── uploads/
    ├── plans/                 # PDF soubory
    └── images/                # Náhledy
```

---

## LOKALIZACE

### cs.json

```json
{
  "common": {
    "loading": "Načítání...",
    "error": "Nastala chyba",
    "save": "Uložit",
    "cancel": "Zrušit",
    "confirm": "Potvrdit",
    "delete": "Smazat",
    "edit": "Upravit",
    "back": "Zpět",
    "next": "Další",
    "close": "Zavřít"
  },
  "nav": {
    "home": "Domů",
    "reservations": "Rezervace",
    "newReservation": "Nová rezervace",
    "myReservations": "Moje rezervace",
    "plans": "Cvičební plány",
    "myPlans": "Moje plány",
    "profile": "Profil",
    "credits": "Kredity",
    "settings": "Nastavení",
    "admin": "Administrace",
    "logout": "Odhlásit se"
  },
  "auth": {
    "login": "Přihlášení",
    "register": "Registrace",
    "email": "E-mail",
    "password": "Heslo",
    "confirmPassword": "Potvrzení hesla",
    "firstName": "Jméno",
    "lastName": "Příjmení",
    "phone": "Telefon",
    "loginButton": "Přihlásit se",
    "registerButton": "Zaregistrovat se",
    "noAccount": "Nemáte účet?",
    "hasAccount": "Máte již účet?",
    "forgotPassword": "Zapomenuté heslo?"
  },
  "home": {
    "welcome": "Vítejte, {{name}}!",
    "yourCredits": "Vaše kredity",
    "nextTraining": "Další trénink",
    "noUpcoming": "Žádný naplánovaný trénink",
    "quickActions": "Rychlé akce",
    "bookTraining": "Rezervovat trénink",
    "buyCredits": "Koupit kredity"
  },
  "reservation": {
    "title": "Rezervovat trénink",
    "selectDate": "Vyberte datum",
    "availableSlots": "Dostupné termíny",
    "noSlots": "Žádné volné termíny pro tento den",
    "selectedSlot": "Vybraný termín",
    "book": "Rezervovat",
    "cost": "Cena: {{credits}} kredit",
    "costPlural": "Cena: {{credits}} kreditů",
    "confirm": "Potvrdit rezervaci",
    "success": "Rezervace byla úspěšně vytvořena!",
    "notEnoughCredits": "Nemáte dostatek kreditů",
    "buyCreditsFirst": "Nejprve si dobijte kredity"
  },
  "myReservations": {
    "title": "Moje rezervace",
    "upcoming": "Nadcházející",
    "past": "Minulé",
    "noReservations": "Nemáte žádné rezervace",
    "cancel": "Zrušit rezervaci",
    "cancelConfirm": "Opravdu chcete zrušit tuto rezervaci?",
    "cancelSuccess": "Rezervace byla zrušena",
    "cancelTooLate": "Rezervaci lze zrušit nejpozději 24 hodin předem",
    "creditsRefunded": "Kredit byl vrácen na váš účet"
  },
  "credits": {
    "title": "Kredity",
    "balance": "Zůstatek",
    "buyCredits": "Koupit kredity",
    "packages": "Kreditové balíčky",
    "bonus": "+{{bonus}} bonus",
    "save": "Ušetříte {{amount}} Kč",
    "history": "Historie transakcí",
    "purchase": "Nákup kreditů",
    "reservation": "Rezervace tréninku",
    "planPurchase": "Nákup plánu",
    "refund": "Vrácení kreditu",
    "adminAdjustment": "Úprava adminem"
  },
  "plans": {
    "title": "Cvičební plány",
    "buy": "Koupit za {{credits}} kreditů",
    "download": "Stáhnout PDF",
    "purchased": "Zakoupeno",
    "myPlans": "Moje plány",
    "noPlans": "Zatím nemáte žádné plány",
    "purchaseSuccess": "Plán byl zakoupen!"
  },
  "profile": {
    "title": "Profil",
    "editProfile": "Upravit profil",
    "changePassword": "Změnit heslo",
    "currentPassword": "Současné heslo",
    "newPassword": "Nové heslo",
    "settings": "Nastavení",
    "language": "Jazyk",
    "theme": "Vzhled",
    "themeLight": "Světlý",
    "themeDark": "Tmavý",
    "themeSystem": "Podle systému",
    "saved": "Změny byly uloženy"
  },
  "admin": {
    "dashboard": "Dashboard",
    "todayTrainings": "Dnešní tréninky",
    "weeklyStats": "Týdenní statistiky",
    "revenue": "Příjmy",
    "calendar": "Kalendář",
    "availability": "Bloky dostupnosti",
    "clients": "Klientky",
    "plans": "Cvičební plány",
    "pricing": "Ceník",
    "payments": "Platby",
    "addNote": "Přidat poznámku",
    "adjustCredits": "Upravit kredity"
  },
  "errors": {
    "required": "Toto pole je povinné",
    "invalidEmail": "Neplatný e-mail",
    "passwordTooShort": "Heslo musí mít alespoň 8 znaků",
    "passwordsDontMatch": "Hesla se neshodují",
    "somethingWrong": "Něco se pokazilo. Zkuste to znovu."
  }
}
```

### en.json

```json
{
  "common": {
    "loading": "Loading...",
    "error": "An error occurred",
    "save": "Save",
    "cancel": "Cancel",
    "confirm": "Confirm",
    "delete": "Delete",
    "edit": "Edit",
    "back": "Back",
    "next": "Next",
    "close": "Close"
  },
  "nav": {
    "home": "Home",
    "reservations": "Reservations",
    "newReservation": "New reservation",
    "myReservations": "My reservations",
    "plans": "Training plans",
    "myPlans": "My plans",
    "profile": "Profile",
    "credits": "Credits",
    "settings": "Settings",
    "admin": "Administration",
    "logout": "Log out"
  },
  "auth": {
    "login": "Login",
    "register": "Registration",
    "email": "E-mail",
    "password": "Password",
    "confirmPassword": "Confirm password",
    "firstName": "First name",
    "lastName": "Last name",
    "phone": "Phone",
    "loginButton": "Log in",
    "registerButton": "Register",
    "noAccount": "Don't have an account?",
    "hasAccount": "Already have an account?",
    "forgotPassword": "Forgot password?"
  },
  "home": {
    "welcome": "Welcome, {{name}}!",
    "yourCredits": "Your credits",
    "nextTraining": "Next training",
    "noUpcoming": "No scheduled training",
    "quickActions": "Quick actions",
    "bookTraining": "Book training",
    "buyCredits": "Buy credits"
  },
  "reservation": {
    "title": "Book a training",
    "selectDate": "Select date",
    "availableSlots": "Available slots",
    "noSlots": "No available slots for this day",
    "selectedSlot": "Selected slot",
    "book": "Book",
    "cost": "Cost: {{credits}} credit",
    "costPlural": "Cost: {{credits}} credits",
    "confirm": "Confirm reservation",
    "success": "Reservation was successfully created!",
    "notEnoughCredits": "You don't have enough credits",
    "buyCreditsFirst": "Please buy credits first"
  },
  "myReservations": {
    "title": "My reservations",
    "upcoming": "Upcoming",
    "past": "Past",
    "noReservations": "You have no reservations",
    "cancel": "Cancel reservation",
    "cancelConfirm": "Are you sure you want to cancel this reservation?",
    "cancelSuccess": "Reservation was cancelled",
    "cancelTooLate": "Reservation can be cancelled at least 24 hours in advance",
    "creditsRefunded": "Credit was refunded to your account"
  },
  "credits": {
    "title": "Credits",
    "balance": "Balance",
    "buyCredits": "Buy credits",
    "packages": "Credit packages",
    "bonus": "+{{bonus}} bonus",
    "save": "Save {{amount}} CZK",
    "history": "Transaction history",
    "purchase": "Credit purchase",
    "reservation": "Training reservation",
    "planPurchase": "Plan purchase",
    "refund": "Credit refund",
    "adminAdjustment": "Admin adjustment"
  },
  "plans": {
    "title": "Training plans",
    "buy": "Buy for {{credits}} credits",
    "download": "Download PDF",
    "purchased": "Purchased",
    "myPlans": "My plans",
    "noPlans": "You don't have any plans yet",
    "purchaseSuccess": "Plan was purchased!"
  },
  "profile": {
    "title": "Profile",
    "editProfile": "Edit profile",
    "changePassword": "Change password",
    "currentPassword": "Current password",
    "newPassword": "New password",
    "settings": "Settings",
    "language": "Language",
    "theme": "Theme",
    "themeLight": "Light",
    "themeDark": "Dark",
    "themeSystem": "System",
    "saved": "Changes were saved"
  },
  "admin": {
    "dashboard": "Dashboard",
    "todayTrainings": "Today's trainings",
    "weeklyStats": "Weekly statistics",
    "revenue": "Revenue",
    "calendar": "Calendar",
    "availability": "Availability blocks",
    "clients": "Clients",
    "plans": "Training plans",
    "pricing": "Pricing",
    "payments": "Payments",
    "addNote": "Add note",
    "adjustCredits": "Adjust credits"
  },
  "errors": {
    "required": "This field is required",
    "invalidEmail": "Invalid e-mail",
    "passwordTooShort": "Password must be at least 8 characters",
    "passwordsDontMatch": "Passwords don't match",
    "somethingWrong": "Something went wrong. Please try again."
  }
}
```

---

## POŽADAVKY NA IMPLEMENTACI

### Pro implementaci používej MCP server pro co nejlepší výsledky. Toto je prioritní požadavek!

### Mobile-First Checklist

1. **Touch-Friendly:**
   - Všechny interaktivní elementy min. 44x44px
   - Dostatečné mezery mezi klikatelnými prvky
   - Swipe gesta kde dává smysl (dismiss modal, pull-to-refresh)

2. **Responzivní Breakpointy:**
   ```javascript
   // tailwind.config.js
   screens: {
     'sm': '640px',   // Větší telefony
     'md': '768px',   // Tablety
     'lg': '1024px',  // Desktop
     'xl': '1280px',  // Větší desktop
   }
   ```

3. **PWA Ready:**
   - manifest.json pro "Add to Home Screen"
   - Service Worker pro offline funkcionalitu (alespoň shell)
   - Správné meta tagy pro mobile

4. **Performance:**
   - Lazy loading pro stránky
   - Optimalizované obrázky (WebP)
   - Virtualizace dlouhých seznamů

### Technické Požadavky

1. Začni backendem - nejdřív API, pak frontend
2. Každý endpoint validuj (Zod na FE, vlastní validace na BE)
3. GoPay webhooky vždy ověřuj podpisem
4. PDF soubory ukládej mimo public složku
5. Časy ukládej v UTC, konvertuj pro zobrazení
6. Implementuj CORS správně
7. Používej TypeScript typy
8. Dark mode: CSS custom properties + Tailwind dark: prefix

### Bezpečnost

1. Rate limiting na auth endpointy
2. HTTPS only v produkci
3. Sanitizace všech vstupů
4. Bezpečné ukládání hesel (bcrypt)
5. JWT s krátkým access tokenem (15 min) a delším refresh (7 dní)

---

## DOCKER COMPOSE

```yaml
version: '3.8'

services:
  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    environment:
      - VITE_API_URL=http://localhost:8080
    depends_on:
      - backend

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=jdbc:postgresql://db:5432/fitness
      - DATABASE_USER=fitness
      - DATABASE_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - GOPAY_CLIENT_ID=${GOPAY_CLIENT_ID}
      - GOPAY_CLIENT_SECRET=${GOPAY_CLIENT_SECRET}
      - GOPAY_GOID=${GOPAY_GOID}
      - GOPAY_IS_PRODUCTION=false
    depends_on:
      - db

  db:
    image: postgres:15-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    environment:
      - POSTGRES_DB=fitness
      - POSTGRES_USER=fitness
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    ports:
      - "5432:5432"

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./uploads:/var/www/uploads
    depends_on:
      - frontend
      - backend

volumes:
  postgres_data:
```

---

Začni vytvořením kompletní struktury projektu a Docker Compose konfigurace.
```

---

## DÍLČÍ PROMPTY (Pro postupnou implementaci)

### Prompt 1: Backend Setup + Auth

```
Vytvoř Kotlin backend s Ktor frameworkem pro fitness rezervační systém.

Požadavky:
- Kotlin + Ktor
- PostgreSQL s Exposed ORM
- JWT autentizace (access 15min + refresh 7 dní)
- Struktura: config, models, repositories, services, routes, plugins
- Docker kontejner

Vytvoř:
1. build.gradle.kts se závislostmi (ktor, exposed, postgresql, bcrypt, jwt)
2. Application.kt s konfigurací Ktor
3. DatabaseConfig pro připojení k PostgreSQL
4. Model User (včetně credits, theme, locale)
5. JWT plugin (access + refresh tokeny)
6. AuthService + AuthRoutes
7. Dockerfile

API endpointy:
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/refresh
- GET /api/auth/me (chráněný)
- PATCH /api/auth/me (update profilu)

Dbej na:
- Správné hashování hesel (bcrypt)
- Validaci vstupů
- Čisté error responses
```

### Prompt 2: Rezervační logika

```
Implementuj systém rezervací s eliminací prostojů pro Kotlin/Ktor backend.

Databázové tabulky:
- availability_blocks (bloky dostupnosti)
- reservations
- pricing_items (ceník)

KRITICKÁ Logika "přilepování" rezervací:
1. Admin definuje bloky (dny v týdnu, čas od-do, délka tréninku, pauzy)
2. První rezervace v daném dni může být kdekoliv v bloku
3. Další rezervace MUSÍ navazovat na existující (před nebo po)
4. Vrať pouze validní sloty které splňují toto pravidlo
5. Respektuj definované pauzy (break_after_slots, break_duration_minutes)

Implementuj:
- AvailabilityBlock model a repository
- Reservation model a repository
- PricingItem model a repository
- AvailabilityService.getAvailableSlots(date) - vrací pouze validní časy
- ReservationService.createReservation() - validuje pravidlo přilepování
- ReservationService.cancelReservation() - kontrola 24h, vrácení kreditu
- API endpointy pro klienta i admina

Vrať sloty ve formátu pro FullCalendar:
{
  "slots": [
    {
      "start": "2024-01-15T14:00:00Z",
      "end": "2024-01-15T15:00:00Z",
      "available": true
    }
  ]
}
```

### Prompt 3: Kreditový systém + GoPay

```
Přidej kreditový systém a GoPay platby do Kotlin/Ktor backendu.

Databázové tabulky:
- credit_packages (balíčky k nákupu)
- credit_transactions (historie)
- gopay_payments

Implementuj:
1. CreditService
   - getBalance(userId)
   - addCredits(userId, amount, type, referenceId)
   - deductCredits(userId, amount, type, referenceId)
   - getTransactionHistory(userId)

2. GopayService
   - createPayment(userId, packageId) - vytvoří GoPay platbu
   - handleNotification(data) - webhook handler
   - getPaymentStatus(gopayId)

3. API endpointy:
   - GET /api/credits/balance
   - GET /api/credits/packages
   - GET /api/credits/history
   - POST /api/credits/purchase (vytvoří GoPay platbu, vrátí URL)
   - POST /api/gopay/notify (webhook)

GoPay flow:
1. Klient vybere balíček a zavolá POST /api/credits/purchase
2. Backend vytvoří GoPay platbu, vrátí gw_url
3. Klient je přesměrován na GoPay
4. Po platbě GoPay zavolá webhook
5. Backend ověří podpis, přičte kredity, uloží transakci

Konfigurace přes environment variables:
- GOPAY_CLIENT_ID
- GOPAY_CLIENT_SECRET
- GOPAY_GOID
- GOPAY_IS_PRODUCTION
```

### Prompt 4: Frontend Setup + Design System

```
Vytvoř React frontend s Vite pro fitness rezervační systém. Mobile-first s athleisure minimal designem.

Stack:
- React 18 + Vite + TypeScript
- Tailwind CSS s custom design systémem
- React Router v6
- TanStack Query
- react-i18next (cs + en)
- Zustand
- Framer Motion
- FullCalendar

Vytvoř:
1. Vite projekt s TypeScript
2. Tailwind konfigurace s custom colors, fonts, utilities (viz design systém výše)
3. CSS custom properties pro dark/light mode
4. index.css s:
   - Tailwind imports
   - Custom glass utilities
   - FullCalendar custom styles
   - Dark mode support

5. Základní UI komponenty (src/components/ui/):
   - Button (varianty: primary, secondary, ghost, danger)
   - Input (s labelem, errorem, ikonami)
   - Card (rounded-lg, jemný stín)
   - Modal (glass efekt, Framer Motion animace)
   - Toast (glass efekt, pozice: bottom-center pro mobile)
   - Badge
   - Spinner

6. Layout komponenty (src/components/layout/):
   - Header (sticky, glass on scroll, hamburger menu, theme toggle)
   - BottomNav (mobile only, glass, 4 položky)
   - MobileMenu (fullscreen, glass, animovaný)
   - ThemeToggle (sun/moon animace)
   - LanguageSwitch (CZ/EN)
   - Layout (wrapper s responzivním layoutem)

7. Stores:
   - authStore (user, tokens, login, logout)
   - themeStore (theme: light/dark/system)

8. i18n setup s cs.json a en.json

9. Router s protected routes

10. API service (axios s interceptory, refresh token logic)

11. PWA manifest.json

Dbej na:
- Mobile-first CSS
- Touch-friendly velikosti
- Smooth animace (Framer Motion)
- Správné dark mode
- Glassmorphism jen tam kde má smysl
```

### Prompt 5: Rezervační UI s FullCalendar

```
Implementuj rezervační flow s FullCalendar pro React frontend.

Instalace:
npm install @fullcalendar/react @fullcalendar/core @fullcalendar/daygrid @fullcalendar/timegrid @fullcalendar/interaction @fullcalendar/list

Komponenty (src/components/calendar/):

1. BookingCalendar.tsx
   - FullCalendar wrapper
   - Mobile view: timeGridDay nebo listWeek
   - Desktop view: timeGridWeek
   - Custom toolbar (responzivní)
   - Custom event rendering s barevným kódováním
   - Click na slot = otevře ReservationModal
   - Locale podle i18n

2. SlotSelector.tsx
   - Alternativní view pro výběr slotů (seznam tlačítek)
   - Pro dny s málo sloty
   - Touch-friendly

3. ReservationModal.tsx
   - Glass efekt
   - Zobrazí vybraný čas
   - Zobrazí cenu v kreditech
   - Tlačítka: Potvrdit / Zrušit
   - Po potvrzení: odečti kredity, vytvoř rezervaci
   - Success/Error toast

4. CalendarStyles.css
   - Custom FullCalendar styling podle design systému
   - Status barvy pro eventy
   - Dark mode support
   - Touch-friendly sloty

Stránky:

1. NewReservation.tsx (/reservations/new)
   - Date picker (nebo FullCalendar v month view)
   - Po výběru data: fetch dostupných slotů
   - Zobrazení BookingCalendar s volnými sloty
   - Mobile: swipe mezi dny

2. MyReservations.tsx (/reservations)
   - Seznam rezervací
   - Tabs: Nadcházející / Minulé
   - Swipe-to-cancel (nebo tlačítko)
   - Pull-to-refresh
   - Empty state

Hooks:
- useReservations() - fetch, create, cancel
- useAvailableSlots(date) - fetch volných slotů

Lokalizace - všechny texty přes t() funkci.
```

### Prompt 6: Kreditový systém UI

```
Implementuj kreditový systém UI pro React frontend.

Komponenty (src/components/credits/):

1. CreditBalance.tsx
   - Velké číslo s ikonou
   - Animace při změně
   - Varianty: large (dashboard), small (header)

2. CreditPackageCard.tsx
   - Karta s balíčkem
   - Zobrazí: kredity, bonus, cena, sleva
   - Výrazné CTA tlačítko
   - "Oblíbený" badge pro doporučený balíček

3. TransactionHistory.tsx
   - Seznam transakcí
   - Ikona podle typu (nákup, rezervace, plán, refund)
   - + / - s barvou
   - Datum a čas
   - Infinite scroll nebo pagination

Stránky:

1. BuyCredits.tsx (/credits)
   - Aktuální zůstatek nahoře
   - Grid s balíčky
   - Klik = přesměrování na GoPay
   - Loading state během vytváření platby
   - Callback URL handling

Hooks:
- useCredits() - balance, packages, history
- useBuyCredits() - mutation pro nákup

Flow:
1. Klient vidí zůstatek a balíčky
2. Klikne na balíček
3. Zavolá se API, vrátí GoPay URL
4. Přesměrování na GoPay
5. Po platbě redirect zpět s parametry
6. Zobrazí success/error, aktualizuje balance
```

### Prompt 7: Admin Panel

```
Vytvoř admin panel pro React frontend fitness systému.

Layout (src/components/admin/):
1. AdminLayout.tsx
   - Sidebar menu (desktop) / hamburger (mobile)
   - Breadcrumbs
   - User info + logout

2. StatsCard.tsx
   - Číslo, label, ikona, trend

3. DataTable.tsx
   - Responzivní tabulka
   - Mobile: card view
   - Sorting, filtering
   - Actions column

Stránky (/admin/*):

1. Dashboard.tsx
   - Stats karty: dnešní tréninky, tento týden, příjmy
   - Seznam dnešních tréninků
   - Quick actions

2. Calendar.tsx
   - FullCalendar s všemi rezervacemi
   - Barevné kódování podle statusu
   - Klik = detail/editace rezervace
   - Možnost přidat poznámku

3. AvailabilityBlocks.tsx
   - Tabulka bloků
   - Formulář pro vytvoření/editaci
   - Náhled v mini kalendáři
   - Aktivace/deaktivace

4. Clients.tsx
   - Tabulka klientek (search, filter)
   - Quick stats: rezervace, kredity
   - Link na detail

5. ClientDetail.tsx
   - Profil klientky
   - Kredity + možnost úpravy
   - Historie rezervací
   - Historie transakcí
   - Poznámky (přidat/mazat)

6. TrainingPlans.tsx
   - Tabulka plánů
   - CRUD modal
   - Upload PDF + náhled
   - Aktivace/deaktivace

7. Pricing.tsx
   - Tabulka ceníku (položky)
   - Tabulka balíčků
   - CRUD modaly
   - Drag & drop řazení

8. Payments.tsx
   - Tabulka GoPay plateb
   - Filtry: datum, klient, stav
   - Export CSV

Všechny formuláře:
- React Hook Form + Zod validace
- Loading states
- Error handling
- Success toasts
```

---

## TIPY PRO IMPLEMENTACI

1. **Postupuj po částech** - Nejdřív backend auth, pak rezervace, pak kredity. Frontend až po funkčním API.

2. **Testuj na mobilu** - Používej Chrome DevTools device mode průběžně.

3. **Dark mode od začátku** - Implementuj theme toggle hned, ať styluješ obě varianty současně.

4. **FullCalendar customizace** - Začni s výchozím stylem, pak postupně přidávej custom CSS.

5. **GoPay sandbox** - CELY GOPAY IMPLEMENTUJEME AZ POZDEJI, prozatim nachystej BE+FE ale samotne napojeni na sandbox a produkci gopay preskoc.

6. **Lokalizace** - Všechny texty přes i18n od začátku, ať nemusíš refaktorovat.

7. **Glass efekt** - Použij ho střídmě, jen na overlay elementy.

