package newbilius.nearbybusinesscardexchanger;

public class InformationData {
    public String Phone;
    public String Name;
    public String Email;

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Name).append("\r\n");
        stringBuilder.append(Phone).append("\r\n");
        if (Email != null)
            stringBuilder.append(Email).append("\r\n");
        return stringBuilder.toString();
    }
}