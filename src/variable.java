package src;

public class variable {
    public String Name;
    public String Type;

    public variable(String name, String type) {
        Name = name;
        Type = type;
    }
    public void print(){
        System.out.println(Type +" "+Name);
    }
}
