import javax.print.DocFlavor;

public class Gosc extends Osoba{
    private int id;
    private String telefon;
    private String email;

    public Gosc(int id, String imie, String nazwisko, String telefon, String email){
        super(imie,nazwisko);
        this.id = id;
        this.telefon = telefon;
        this.email = email;
    }
    public int getId(){
        return id;
    }
    public String getTelefon(){
        return telefon;
    }
    public String getEmail() { return email;}

    @Override
    public void wyswietlDane(){
        System.out.println("ID " + id + " " + getImie() + " " + getNazwisko() + " Telefon " + telefon + " | E-mail: " + email);
    }
}
