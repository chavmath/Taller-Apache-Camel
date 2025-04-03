import org.apache.camel.builder.RouteBuilder;

public class hello extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("file:input?noop=true")
            .to("file:output")
            .log("Archivo ${file:name} copiado a la carpeta output");
    }
}