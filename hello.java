import org.apache.camel.builder.RouteBuilder;

public class hello extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("file:input?noop=true")
            .process(exchange -> {
                // Aquí se define la lógica para determinar el tipo de cliente
                // Este es un ejemplo donde el nombre del archivo contiene "VIP" o "regular"
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);

                String subFolder = "regular"; // Valor por defecto

                // Lógica para determinar el tipo de cliente basado en el nombre del archivo
                if (fileName.contains("VIP")) {
                    subFolder = "VIP";
                } else if (fileName.contains("regular")) {
                    subFolder = "regular";
                }

                // Establecer el destino en el encabezado de la ruta para que el archivo vaya a la subcarpeta correspondiente
                exchange.getIn().setHeader("destinationFolder", "file:output/" + subFolder);
            })
            .to("direct:moveFile");

        // Ruta que mueve el archivo a la subcarpeta determinada
        from("direct:moveFile")
            .toD("${header.destinationFolder}")
            .log("Archivo ${file:name} movido a la carpeta ${header.destinationFolder}");
    }
}
