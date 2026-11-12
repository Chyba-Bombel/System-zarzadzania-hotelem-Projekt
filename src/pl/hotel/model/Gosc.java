package pl.hotel.model;

public class Gosc extends Osoba{
    private int id;
    private String telefon;
    private String email;
    private final boolean archiwalny;
    private final java.time.LocalDateTime archiwizowano;
    private final String archiwizowalLogin;

    public Gosc(int id, String imie, String nazwisko, String telefon, String email){
        this(id,imie,nazwisko,telefon,email,false,null,null);
    }
    public Gosc(int id,String imie,String nazwisko,String telefon,String email,boolean archiwalny,java.time.LocalDateTime czas,String login){
        super(imie,nazwisko);
        this.archiwalny=archiwalny;
        this.archiwizowano=czas;
        this.archiwizowalLogin=login;
        this.id = id;
        this.telefon = telefon;
        this.email = email;
    }
    public boolean isArchiwalny(){return archiwalny;}
    public java.time.LocalDateTime getArchiwizowano(){return archiwizowano;}
    public String getArchiwizowalLogin(){return archiwizowalLogin;}
    public int getId(){
        return id;
    }
    public String getTelefon(){
        return telefon;
    }
    public String getEmail() { return email;}

    @Override
    public void wyswietlDane(){
        System.out.println("ID " + id + " " + getImie() + " " + getNazwisko() + " Telefon " + telefon + " | E-mail: " + email + (archiwalny?" | ARCHIWALNY":""));
    }
}
