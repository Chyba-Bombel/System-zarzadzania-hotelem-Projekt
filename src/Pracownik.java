public class Pracownik extends Osoba{
    private String stanowisko;

    public Pracownik(String imie, String nazwisko, String stanowisko){
        super(imie, nazwisko);
        this.stanowisko = stanowisko;
    }
    public String getStanowisko(){
        return stanowisko;
    }
    @Override
    public void wyswietlDane() {
        System.out.println(getImie() + " " + getNazwisko()
                + " | Stanowisko: " + stanowisko);
    }
}
