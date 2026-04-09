public class EnumProbe {
  public static void main(String[] args) throws Exception {
    Class<?> c = Class.forName("com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType");
    Object[] constants = c.getEnumConstants();
    if (constants == null) return;
    for (Object o : constants) {
      System.out.println(String.valueOf(o));
    }
  }
}
