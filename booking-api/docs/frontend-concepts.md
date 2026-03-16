# Frontend-koncept — BookingAPI

En strukturerad guide till alla viktiga frontend-koncept som används i projektet.
Varje koncept förklaras kort, kopplas till projektet och avslutas med en intervjuförklaring.

---

## 1. React-grunder

### Komponenter (Components)

- **Vad det är** — En komponent är en återanvändbar byggsten i React. Den tar emot data, hanterar sitt eget tillstånd och returnerar JSX (HTML-liknande syntax) som beskriver vad som ska visas på skärmen.
- **I projektet** — Varje sida (LoginPage, RoomsPage, BookingsPage) och varje UI-del (ConfirmModal, StatusBadge) är en egen komponent. Det gör att varje fil har ett enda ansvar och är lätt att testa och förstå.
- **På intervju** — "En komponent i React är en isolerad, återanvändbar UI-enhet som ansvarar för sin egen rendering och sitt eget tillstånd."

### JSX

- **Vad det är** — JSX är en syntax-extension som låter dig skriva HTML-liknande kod direkt i JavaScript/TypeScript. React omvandlar det till vanliga JavaScript-anrop under huven.
- **I projektet** — All UI-kod i varje komponent skrivs med JSX. Exempelvis renderar BookingsPage en tabell med bokningar genom att mappa över en array direkt i JSX.
- **På intervju** — "JSX låter mig beskriva UI deklarativt i JavaScript. React tar hand om att uppdatera DOM:en effektivt när data ändras."

### Props

- **Vad det är** — Props (properties) är data som skickas från en föräldrakomponent till en barnkomponent. De är read-only — barnet kan inte ändra dem.
- **I projektet** — När RoomsPage renderar en lista används props för att skicka rum-data ner till varje rad-komponent. ConfirmModal tar emot en `onConfirm`-callback och en `message`-text via props.
- **På intervju** — "Props är mekanismen för att skicka data och callbacks neråt i komponentträdet. De gör komponenter konfigurerbara och återanvändbara."

### State (useState)

- **Vad det är** — State är data som lever inuti en komponent och kan ändras över tid. När state ändras renderar React om komponenten automatiskt.
- **I projektet** — LoginPage har state för email och password (formulärfälten). RoomsPage har state för listan av rum som hämtas från API:et. Laddningsindikatorer styrs av en `loading`-state.
- **På intervju** — "useState ger en komponent lokalt tillstånd. När det uppdateras triggas en omrendering, vilket är Reacts kärna för reaktiv UI."

### useEffect

- **Vad det är** — En hook som kör sidoeffekter (API-anrop, subscriptions, DOM-manipulationer) efter att komponenten har renderats. Den tar en dependency-array som styr när effekten körs igen.
- **I projektet** — Varje sida som hämtar data (RoomsPage, BookingsPage, UsersPage, DashboardPage) använder useEffect med en tom dependency-array `[]` för att hämta data en gång vid mount.
- **På intervju** — "useEffect hanterar sidoeffekter i funktionskomponenter. Jag använder det för att hämta data från API:et när en sida laddas."

### useContext

- **Vad det är** — En hook som ger åtkomst till data som delas via React Context, utan att behöva skicka props genom varje nivå i komponentträdet.
- **I projektet** — AuthContext delar användarens token, email och roll med hela appen. Vilken komponent som helst kan anropa `useContext(AuthContext)` för att kontrollera om användaren är inloggad eller har admin-roll.
- **På intervju** — "useContext låter mig konsumera globalt tillstånd utan prop drilling. I mitt projekt använder jag det för att göra autentiseringsdata tillgänglig överallt."

### Villkorlig rendering (Conditional Rendering)

- **Vad det är** — Att visa eller dölja delar av UI:t baserat på ett villkor. Vanliga mönster är `{condition && <Component />}` och ternary-operatorn.
- **I projektet** — Admin-knappar (skapa rum, radera användare) visas bara om `role === "ADMIN"`. ProtectedRoute visar sidan om token finns, annars redirectar den till login.
- **På intervju** — "Villkorlig rendering låter mig anpassa vad användaren ser baserat på data — till exempel dölja admin-funktioner för vanliga användare."

---

## 2. TypeScript i React

### Varför TypeScript

- **Vad det är** — TypeScript är en superset av JavaScript som lägger till statisk typning. Komplilatorn fångar fel innan koden körs.
- **I projektet** — Alla API-svar har definierade typer (BookingResponse, RoomResponse osv.) som matchar backendets DTOs exakt. Om backend ändrar ett fält upptäcker TypeScript-komplilatorn felet direkt.
- **På intervju** — "TypeScript ger mig typsäkerhet som fångar buggar vid kompilering istället för runtime. Det är särskilt värdefullt vid API-integration där frontend och backend måste vara i synk."

### Interfaces och Types som speglar backend-DTOs

- **Vad det är** — I `src/types/` definieras TypeScript-interfaces som exakt matchar backendets Java-records (BookingResponse, RoomRequest osv.).
- **I projektet** — När Axios hämtar data från `/api/rooms` typas svaret som `RoomResponse[]`. Autokomplettering fungerar direkt och felaktiga fältnamn markeras röda i editorn.
- **På intervju** — "Jag skapar TypeScript-typer som speglar backendets DTOs. Det ger mig kompileringsskydd mot kontraktsbrott mellan frontend och backend."

---

## 3. Projektstruktur

### Mappkonventioner

- **Vad det är** — En förutsägbar mappstruktur som separerar ansvar:
  - `src/api/` — Alla API-anrop (en fil per resurs)
  - `src/pages/` — Sidkomponenter kopplade till routes
  - `src/components/` — Återanvändbara UI-komponenter
  - `src/hooks/` — Custom hooks
  - `src/context/` — React Context providers
  - `src/types/` — TypeScript-typer
- **I projektet** — Att söka efter "var görs API-anropet för rum?" leder direkt till `src/api/rooms.ts`. Att söka efter "var renderas bokningssidan?" leder till `src/pages/BookingsPage.tsx`. Strukturen är självdokumenterande.
- **På intervju** — "Jag organiserar koden efter ansvar — API-logik, sidor, återanvändbara komponenter och typer har separata mappar. Det gör kodbasen navigerbar och skalbar."

### Separation of Concerns

- **Vad det är** — Principen att varje modul/fil ska ha ett enda ansvar. API-logik blandas inte med UI-rendering. State management blandas inte med presentationslogik.
- **I projektet** — `src/api/rooms.ts` vet hur man pratar med backend men vet inget om UI. `RoomsPage.tsx` vet hur rum ska visas men vet inget om HTTP-anrop — den anropar bara funktioner från api-lagret.
- **På intervju** — "Jag separerar API-kommunikation från UI-logik. Det gör varje del testbar isolerat och gör det enkelt att byta ut t.ex. Axios mot fetch utan att röra någon komponent."

---

## 4. Routing

### React Router

- **Vad det är** — Ett bibliotek som hanterar navigation i en single-page application (SPA). Istället för att webbläsaren laddar en ny HTML-sida vid varje klick, uppdaterar React Router bara den del av sidan som ändras.
- **I projektet** — Routes definieras centralt: `/login`, `/register` (publika), `/dashboard`, `/rooms`, `/bookings`, `/users` (skyddade). Alla skyddade routes wrappar sina element med en ProtectedRoute-komponent.
- **På intervju** — "React Router ger mig klientbaserad routing i en SPA. Användaren navigerar snabbt utan att sidan laddas om, medan URL:en fortfarande uppdateras korrekt."

### ProtectedRoute-mönstret

- **Vad det är** — En wrapper-komponent som kontrollerar om användaren är inloggad innan den renderar barnsidan. Om inte, redirectas användaren till login.
- **I projektet** — Alla sidor utom Login och Register wrappas med ProtectedRoute. Den läser token från AuthContext — finns ingen token skickas användaren till `/login`.
- **På intervju** — "ProtectedRoute är en komponent som fungerar som en grindvakt. Den kontrollerar autentisering innan en sida visas och redirectar till login om token saknas."

### useNavigate

- **Vad det är** — En hook som ger programmatisk navigation. Istället för att användaren klickar på en länk kan koden navigera efter t.ex. ett lyckat API-anrop.
- **I projektet** — Efter lyckad login navigerar koden till `/dashboard`. Efter att en ny bokning skapats navigeras användaren tillbaka till bokningslistan.
- **På intervju** — "useNavigate ger mig programmatisk navigering — jag kan omdirigera användaren efter en lyckad inloggning eller formulärinskick."

---

## 5. API-kommunikation

### Axios

- **Vad det är** — Ett HTTP-bibliotek för JavaScript som förenklar API-anrop. Det har stöd för interceptors, automatisk JSON-parsning och bra felhantering jämfört med det inbyggda fetch-API:et.
- **I projektet** — En central Axios-instans skapas i `src/api/client.ts` med `baseURL` som pekar på backend. Alla API-filer (rooms.ts, bookings.ts osv.) importerar denna instans istället för att konfigurera URL:er överallt.
- **På intervju** — "Jag använder en central Axios-instans med fördefinierad baseURL och interceptors. Det eliminerar duplicerad konfiguration och ger en enda plats för att hantera autentisering och fel."

### Request Interceptor

- **Vad det är** — En funktion som körs automatiskt innan varje utgående HTTP-request. Den kan modifiera requesten, t.ex. lägga till headers.
- **I projektet** — Interceptorn läser JWT-token från localStorage och lägger till `Authorization: Bearer <token>` på varje request. Ingen komponent behöver tänka på att skicka med token manuellt.
- **På intervju** — "Jag har en Axios request interceptor som automatiskt bifogar JWT-token till varje API-anrop. Det centraliserar autentiseringslogiken till en enda plats."

### Response Interceptor

- **Vad det är** — En funktion som körs automatiskt när ett HTTP-svar kommer tillbaka. Den kan hantera fel globalt innan de når den anropande koden.
- **I projektet** — Om backend svarar med 401 (ogiltig/utgången token) loggas användaren ut automatiskt och redirectas till login. 403 visar ett "åtkomst nekad"-meddelande. Andra fel visar en generisk toast-notifikation.
- **På intervju** — "Min response interceptor hanterar 401 och 403 globalt — det betyder att ingen enskild komponent behöver ha logik för utgångna tokens."

### async/await

- **Vad det är** — Syntaktiskt socker för att hantera asynkrona operationer (som API-anrop) på ett läsbart sätt, istället för .then()-kedjor.
- **I projektet** — Alla API-funktioner och useEffect-callbacks använder async/await. Exempelvis: `const rooms = await getRooms()` istället för `getRooms().then(res => ...)`.
- **På intervju** — "Jag använder async/await för alla asynkrona operationer. Det gör koden linjär och lättare att läsa än callback-kedjor."

---

## 6. Autentisering och JWT

### Vad är JWT (JSON Web Token)

- **Vad det är** — En token i tre delar (header.payload.signature) kodad i Base64. Payload:en innehåller claims som användarens email och roll. Signaturen verifierar att token inte har manipulerats.
- **I projektet** — Backend genererar en JWT vid inloggning. Frontend sparar den och skickar med den i varje API-anrop. Backend verifierar signaturen vid varje request — ingen session behövs på servern.
- **På intervju** — "JWT ger stateless autentisering. Backend behöver inte spara sessioner — allt som behövs för att verifiera en användare finns i token, signerad med en hemlig nyckel."

### localStorage

- **Vad det är** — Webbläsarens nyckel-värde-lagring som överlever sidladdningar och flikstängningar (till skillnad från sessionStorage).
- **I projektet** — JWT-token sparas i localStorage vid inloggning. Vid sidladdning läser AuthContext token från localStorage och återställer inloggat tillstånd. Vid logout rensas localStorage.
- **På intervju** — "Jag sparar JWT i localStorage för att behålla inloggningen vid sidladdning. AuthContext läser token vid uppstart och återställer användarens tillstånd."

### JWT-dekodning på klienten

- **Vad det är** — Payload-delen av en JWT är bara Base64-kodad (inte krypterad). Frontend kan avkoda den för att läsa t.ex. email och roll utan att kontakta backend.
- **I projektet** — En liten hjälpfunktion tar token-strängen, avkodar payload:en med `atob()` och returnerar email och roll. Dessa används för att styra UI:t (visa/dölja admin-funktioner).
- **På intervju** — "Jag avkodar JWT-payload:en på klienten för att få fram användarens roll och email. Det gör att UI:t kan anpassa sig direkt utan extra API-anrop."

### AuthContext

- **Vad det är** — En React Context som håller autentiseringstillståndet (token, email, roll) och exponerar funktioner (login, register, logout) till hela appen.
- **I projektet** — AuthContext wrappas runt hela appen. Vilken komponent som helst kan anropa `const { user, login, logout } = useAuth()` för att kolla inloggningsstatus eller logga ut.
- **På intervju** — "AuthContext centraliserar all autentiseringslogik. Varje komponent kan komma åt användarens tillstånd och auth-funktioner via en custom hook."

---

## 7. State Management

### React Context API

- **Vad det är** — Reacts inbyggda mekanism för att dela data genom komponentträdet utan att skicka props manuellt genom varje nivå (prop drilling).
- **I projektet** — Används för autentiseringstillstånd (AuthContext). Token, email och roll behövs av navigeringen, av ProtectedRoute, av sidor som visar olika UI baserat på roll — att skicka det som props genom 5 nivåer vore opraktiskt.
- **På intervju** — "Jag använder React Context API för global state som autentisering. Det undviker prop drilling och gör att alla komponenter har tillgång till auth-data."

### Varför inte Redux/Zustand?

- **Vad det är** — Redux och Zustand är externa state management-bibliotek. De löser problem med komplex global state, men lägger till overhead.
- **I projektet** — Projektet har bara ett globalt tillstånd — autentisering. Resten (rumlista, bokningslista) är lokal page-state som hämtas med useEffect. Context API räcker gott och hålller beroendena minimala.
- **På intervju** — "Jag valde Context API framför Redux eftersom projektets enda globala state är autentisering. Att introducera Redux hade lagt till komplexitet utan att lösa ett faktiskt problem."

---

## 8. Rollbaserad UI

### Conditional Rendering baserat på roll

- **Vad det är** — Att visa eller dölja UI-element beroende på användarens roll (ADMIN eller USER).
- **I projektet** — Admin ser knappar för att skapa/redigera/radera rum och användare. USER ser bara läs-vyer och sina egna bokningar. Navigeringslänken "Users" visas bara för admin.
- **På intervju** — "Jag renderar admin-funktioner villkorligt baserat på rollen som avkodas från JWT. Det ger en anpassad upplevelse utan att ladda om sidan."

### Frontend-skydd kontra backend-skydd

- **Vad det är** — Frontend kan dölja knappar, men en teknisk användare kan fortfarande anropa API:et direkt. Därför måste backend ALLTID verifiera rollen — frontend-skyddet är bara UX.
- **I projektet** — Även om "Create Room"-knappen är dold för USER-rollen har backend `@PreAuthorize("hasRole('ADMIN')")` som blockerar anropet med 403 om någon försöker direkt.
- **På intervju** — "Frontend-rollkontroll är UX — den gör gränssnittet rent. Men säkerheten sitter i backend med @PreAuthorize. Jag litar aldrig enbart på klienten."

---

## 9. Komponentarkitektur

### Pages vs Components

- **Vad det är** — Pages är helskärmskomponenter kopplade till en route (/rooms, /bookings). Components är mindre, återanvändbara delar som kan användas på flera sidor.
- **I projektet** — `RoomsPage` är en page — den hämtar data och renderar hela sidan. `ConfirmModal` är en component — den kan användas för att bekräfta borttagning av rum, bokningar och användare.
- **På intervju** — "Jag skiljer på pages (route-bundna sidor) och components (återanvändbara delar). Det gör att UI-element kan delas mellan sidor utan kodduplicering."

### Återanvändbara komponenter

- **Vad det är** — Komponenter designade för att användas i flera sammanhang. De tar emot konfiguration via props och har inget hårdkodat beroende till en specifik sida.
- **I projektet** — ConfirmModal används för alla raderingsbekräftelser. StatusBadge visar bokningsstatus med rätt färg (grön för CONFIRMED, röd för CANCELLED) på flera ställen.
- **På intervju** — "Jag extraherar gemensam UI-logik till återanvändbara komponenter. Det minskar kodduplicering och gör att ändringar propageras automatiskt överallt."

### AppShell-mönstret

- **Vad det är** — En layout-komponent som ger persistent navigation (sidebar, header) runt alla skyddade sidor. Bara sidinnehållet byts ut vid navigation — sidofältet och headern renderas en gång.
- **I projektet** — AppShell visar sidofält med navigationslänkar och en header med användarens email och en logout-knapp. Alla protected routes renderas inuti AppShell:en.
- **På intervju** — "Jag använder ett AppShell-mönster där layout och navigation renderas en gång och bara sidinnehållet byts ut. Det ger snabb navigering och en konsekvent upplevelse."

---

## 10. Formulärhantering och validering

### Controlled Inputs

- **Vad det är** — Formulärfält vars värde styrs av React-state. Vid varje knapptryckning uppdateras state, som i sin tur uppdaterar fältet. React är "single source of truth".
- **I projektet** — LoginPage har `const [email, setEmail] = useState("")` och inputfältet har `value={email}` och `onChange={e => setEmail(e.target.value)}`. Formulärdatan är alltid tillgänglig i state för att skicka till API:et.
- **På intervju** — "Jag använder controlled inputs där React-state är single source of truth för formulärdata. Det ger mig full kontroll över validering och submit."

### Klient-validering som speglar backend

- **Vad det är** — Att applicera samma valideringsregler i frontend som backend redan har (t.ex. email-format, minimilängd på lösenord). Det ger snabb feedback utan att vänta på ett API-svar.
- **I projektet** — RegisterPage validerar att email är giltig och att lösenordet är minst 6 tecken — exakt samma regler som `RegisterRequest` i backend. Om valideringen misslyckas visas felet direkt utan att ett request skickas.
- **På intervju** — "Klient-validering ger direkt feedback. Jag speglar backendets valideringsregler för att minimera onödiga API-anrop, men backend validerar alltid som sista försvarslinje."

---

## 11. Felhantering

### Global felhantering med Axios interceptor

- **Vad det är** — En centraliserad plats som fångar alla API-fel innan de når komponenterna. Olika HTTP-statuskoder hanteras på olika sätt.
- **I projektet**:
  - **401** — Token saknas eller har gått ut → automatisk utloggning och redirect till /login
  - **403** — Användaren har inte rätt roll → "Åtkomst nekad"-notifikation
  - **409** — Bokningskonflikt → specifikt felmeddelande visas i formuläret
  - **400** — Valideringsfel → felmeddelanden från backend visas
  - **500** — Oväntat fel → generisk "Något gick fel"-notifikation
- **På intervju** — "Jag har en global Axios interceptor som hanterar felkoder centralt. 401 triggar automatisk utloggning, 409 visar konfliktmeddelande, och allt annat visas som en notifikation."

### ErrorResponse-typ

- **Vad det är** — En TypeScript-typ som matchar backendets felformat: `{ status: number, message: string, timeStamp: string }`.
- **I projektet** — När ett API-anrop misslyckas parsas response-bodyn som ErrorResponse. Fältet `message` visas direkt för användaren. TypeScript säkerställer att vi aldrig accessar ett fält som inte finns.
- **På intervju** — "Backend returnerar ett konsekvent felformat. Jag har en matchande TypeScript-typ som gör felhanteringen typsäker."

---

## 12. Laddningstillstånd och UX

### Loading States

- **Vad det är** — UI-feedback som visar att data håller på att laddas. Utan det ser sidan tom eller trasig ut under de millisekunder det tar att hämta data.
- **I projektet** — Varje sida som gör API-anrop har `const [loading, setLoading] = useState(true)`. Medan loading är true visas en spinner eller skeleton. När data laddats klart renderas det riktiga innehållet.
- **På intervju** — "Jag visar laddningsindikatorer medan data hämtas. Det förbättrar upplevd prestanda och förhindrar att användaren ser en tom sida."

### Empty States

- **Vad det är** — Ett dedikerat UI för när det inte finns någon data att visa — istället för en tom tabell visas ett vänligt meddelande.
- **I projektet** — Om en USER inte har några bokningar visas "Du har inga bokningar ännu" istället för en tom tabell. Om admin tittar på rum och inga finns visas "Inga rum skapade".
- **På intervju** — "Empty states ger tydlig feedback istället för tomma tabeller. Det är en liten detalj som visar att jag tänker på användarupplevelsen."

---

## 13. Miljövariabler

### .env-filer och VITE_API_URL

- **Vad det är** — Miljövariabler definieras i `.env`-filer och läses av Vite vid byggtillfället. Variabler med prefixet `VITE_` exponeras till frontend-koden.
- **I projektet** — `VITE_API_URL=http://localhost:8080` definierar backendets adress. Axios-instansen använder den som baseURL. I Docker-miljö ändras den till det interna container-nätverket.
- **På intervju** — "Jag använder miljövariabler för att konfigurera API-URL:en. Det gör att samma kodbas fungerar i utveckling och produktion utan kodändringar."

### Hemligheter hör till backend

- **Vad det är** — Frontend-kod skickas till webbläsaren och kan läsas av vem som helst. Hemliga nycklar (JWT-secret, databas-lösenord) får aldrig finnas i frontend-kod.
- **I projektet** — JWT-signeringen sker enbart på backend. Frontend har bara den publika token:en. `.env`-filer som innehåller hemligheter läggs i `.gitignore`.
- **På intervju** — "Jag lagrar aldrig hemligheter i frontend. JWT-signeringen sker på backend och frontend har bara den publika token:en."

---

## 14. Byggprocess och deploy

### Vite

- **Vad det är** — Ett snabbt build-verktyg för moderna webbprojekt. Det ger snabb utvecklingsserver (HMR — hot module replacement) och optimerad produktionsbygge.
- **I projektet** — `npm run dev` startar utvecklingsservern med HMR. `npm run build` genererar optimerade statiska filer (HTML, CSS, JS) i `dist/`-mappen.
- **På intervju** — "Jag använder Vite som build-verktyg. Det ger snabb utveckling med HMR och producerar optimerade statiska filer för produktion."

### Nginx som static file server

- **Vad det är** — Nginx serverar de statiska filerna (HTML/CSS/JS) som Vite byggt. Den kan också fungera som reverse proxy och skicka API-anrop vidare till backend.
- **I projektet** — Docker-imagen bygger React-appen med Node, kopierar output:en till en Nginx-container och konfigurerar Nginx att proxya `/api/*`-requests till backend-containern.
- **På intervju** — "I produktion serveras React-appen av Nginx. API-anrop proxyas till backend-containern, vilket eliminerar CORS-problem och gör att allt körs under samma domän."

### Docker multi-stage build

- **Vad det är** — En Dockerfile med två steg: ett bygger applikationen, det andra kopierar bara resultatet till en minimal image. Slutresultatet blir en liten image utan build-verktyg.
- **I projektet** — Steg 1 (Node) kör `npm run build`. Steg 2 (Nginx) kopierar de statiska filerna. Samma mönster används för backend-imagen (Maven-bygge + JRE).
- **På intervju** — "Jag använder Docker multi-stage build för att hålla production-imagen liten. Build-verktyg finns bara i det temporära byggsteget."

### Reverse Proxy

- **Vad det är** — En server (Nginx) som tar emot alla requests och dirigerar dem: statiska filer hanteras direkt, API-anrop skickas vidare till backend-servern.
- **I projektet** — Nginx-konfigurationen skickar `/api/*`-requests till `http://backend:8080`. Frontend och backend delar samma domän/port utåt, vilket eliminerar CORS-konfiguration.
- **På intervju** — "Nginx fungerar som reverse proxy — den serverar frontend-filerna direkt och proxar API-anrop till Spring Boot-backend. Det undviker CORS och förenklar deploy."

---

## 15. Portfolio-perspektiv

### Clean commit history

- **Vad det är** — Commit-meddelanden som tydligt beskriver vad varje ändring gör, grupperade i logiska steg. Recruiters läser ofta git-loggen för att bedöma arbetssätt.
- **I projektet** — Varje fas i roadmapen avslutas med en commit (`feat: add authentication`, `feat: add room management` osv.). Det visar ett inkrementellt, strukturerat arbetssätt.
- **På intervju** — "Jag commitar i logiska steg med beskrivande meddelanden. Det visar att jag arbetar strukturerat och att projektet har byggts steg för steg."

### Monorepo-struktur

- **Vad det är** — Backend och frontend lever i samma Git-repo men i separata mappar. Det gör det enkelt för en granskare att klona ett repo och se hela systemet.
- **I projektet** — `booking-api/` (backend) och `booking-frontend/` (frontend) ligger sida vid sida i root. `docker compose up --build` startar allt.
- **På intervju** — "Jag använder en monorepo-struktur där backend och frontend delar samma repo. En recruiter kan klona ett repo, köra docker compose och se hela systemet."

### README som portfolio-presentation

- **Vad det är** — README.md är det första en recruiter ser. Den ska snabbt förklara vad projektet gör, vilken tech stack som används, och hur man kör det.
- **I projektet** — README beskriver att det är ett fullstack bokningssystem, listar tech stack (Spring Boot + React + PostgreSQL + Docker), och har en enkel "How to Run"-sektion.
- **På intervju** — "Min README fungerar som en snabbintroduktion till projektet. Den beskriver arkitekturen, tech stack och hur man startar allt med ett kommando."

### Vad recruiters letar efter

- **Vad det är** — Recruiters och tekniska granskare tittar inte bara på att koden fungerar. De bedömer:
  - Kodstruktur och separation of concerns
  - Konsekvent namngivning och mappstruktur
  - Commit-historik
  - Felhantering och edge cases
  - Typsäkerhet (TypeScript)
  - Att frontend och backend kommunicerar korrekt
  - Att det går att köra projektet lokalt utan manuella steg
- **I projektet** — Alla dessa punkter adresseras: TypeScript-typer, mappstruktur, global felhantering, rollbaserad UI, Docker Compose och en tydlig README.
- **På intervju** — "Jag har byggt projektet med samma struktur och kvalitetskrav som ett riktigt team-projekt. Det visar att jag inte bara kan skriva kod — jag kan bygga system."
