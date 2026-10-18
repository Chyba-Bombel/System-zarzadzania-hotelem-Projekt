public class Gosc extends Osoba{
    private int id;
    private String telefon;

    public Gosc(int id, String imie, String nazwisko, String telefon){
        super(imie,nazwisko);
        this.id = id;
        this.telefon = telefon;
    }
    public int getId(){
        return id;
    }
    public String getTelefon(){
        return telefon;
    }

    @Override
    public void wyswietlDane(){
        System.out.println("ID " + id + " " + getImie() + " " + getNazwisko() + " Telefon " + telefon);
    }
}
