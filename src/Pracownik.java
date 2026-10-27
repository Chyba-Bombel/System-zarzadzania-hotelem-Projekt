public class Pracownik extends Osoba{
    private final long id;
    private final String login;
    private final String stanowisko;


    public Pracownik(String imie, String nazwisko, String stanowisko){
        this(0,imie,nazwisko,"",stanowisko);
    }
    public Pracownik(long id, String imie, String nazwisko, String login, String rola){
        super(imie, nazwisko);
        this.id = id;
        this.login = login;
        this.stanowisko = rola;
    }
    public long getId(){
        return id;
    }
    public String getLogin(){
        return login;
    }
    public String getStanowisko(){
        return stanowisko;
    }
    public String getRola(){
        return stanowisko;
    }
    @Override
    public void wyswietlDane() {
        System.out.println(getImie() + " " + getNazwisko()
                + " | Stanowisko: " + stanowisko);
    }
}
