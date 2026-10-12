**Temat projektu: System zarządzania hotelem**

**Technologie:** Java 17+, MySQL 8+, JDBC

**1\. Opis całego projektu**

System zarządzania hotelem będzie aplikacją działającą w konsoli. Istnieje również możliwość stworzenia interfejsu graficznego, jednak jego implementacja będzie zależała od dostępnego czasu.

Głównym celem aplikacji będzie usprawnienie obsługi rezerwacji, zarządzania pokojami, gośćmi oraz płatnościami. Wszystkie najważniejsze informacje będą przechowywane w bazie danych MySQL, dzięki czemu dane pozostaną dostępne nawet po ponownym uruchomieniu programu.

**2\. Opis działania użytkowników i logowania**

System będzie obsługiwał dwa rodzaje użytkowników: administratora systemu oraz pracownika recepcji.

**Administrator** będzie zarządzał całym systemem. Będzie miał możliwość dodawania i usuwania kont pracowników, zarządzania pokojami, gośćmi oraz rezerwacjami. Będzie również posiadał dostęp do wszystkich funkcji administracyjnych systemu.

**Recepcjonista** będzie odpowiedzialny za codzienną obsługę hotelu, w tym obsługę gości, tworzenie i modyfikowanie rezerwacji, zameldowania, wymeldowania oraz przyjmowanie płatności. Jego uprawnienia będą ograniczone do funkcji niezbędnych do wykonywania obowiązków służbowych.

System będzie zabezpieczony mechanizmem logowania. Hasła użytkowników nie będą przechowywane w postaci jawnego tekstu. Zamiast tego zostaną zapisane w postaci bezpiecznych skrótów kryptograficznych z wykorzystaniem odpowiedniej metody przeznaczonej do przechowywania haseł. Dzięki temu w przypadku wycieku bazy danych ryzyko poznania oryginalnych haseł zostanie ograniczone.

**3\. Główny opis działania programu**

Program będzie składał się z kilku modułów odpowiadających za poszczególne obszary funkcjonowania hotelu.

**3.1. Moduł gości**

Moduł gości będzie umożliwiał dodawanie nowych gości do systemu, przeglądanie ich danych oraz aktualizowanie informacji. Będzie również pozwalał na powiązanie gościa z odpowiednią rezerwacją i zapisanie planowanego terminu zameldowania oraz wymeldowania.

**3.2. Moduł pokoi**

Moduł pokoi będzie umożliwiał zarządzanie pokojami hotelowymi. Administrator będzie mógł dodawać nowe pokoje, edytować ich dane oraz zmieniać ich status, na przykład na dostępny, zajęty lub wyłączony z użytkowania.

**3.3. Moduł rezerwacji**

Moduł rezerwacji będzie umożliwiał tworzenie, przeglądanie, modyfikowanie oraz anulowanie rezerwacji. Podczas tworzenia rezerwacji system będzie sprawdzał dostępność wybranego pokoju w określonym terminie, aby zapobiec nakładaniu się rezerwacji.

**3.4. Moduł zameldowania i wymeldowania**

Moduł ten będzie umożliwiał rejestrowanie zameldowania gościa w hotelu oraz jego późniejszego wymeldowania. System będzie zapisywał odpowiednie daty i aktualizował informacje o stanie realizacji rezerwacji.

**3.5. Moduł płatności**

Moduł płatności będzie umożliwiał rejestrowanie wpłat za pobyt, kontrolowanie statusu płatności oraz sprawdzanie, czy należność za rezerwację została uregulowana. System będzie przechowywał informacje o kwocie, dacie oraz metodzie płatności.

**3.6. Moduł użytkowników i uprawnień**

Moduł ten będzie odpowiadał za zarządzanie kontami użytkowników oraz kontrolowanie ich uprawnień. Administrator będzie mógł tworzyć i usuwać konta pracowników, natomiast recepcjoniści będą korzystać wyłącznie z funkcji dostępnych dla ich roli.

**3.7. Moduł bazy danych**

Baza danych MySQL będzie przechowywała informacje o użytkownikach, gościach, pokojach, rezerwacjach i płatnościach. Połączenie aplikacji z bazą danych zostanie zrealizowane przy użyciu JDBC. Pozwoli to na trwałe zapisywanie danych oraz ich odczytywanie i aktualizowanie podczas działania programu.
