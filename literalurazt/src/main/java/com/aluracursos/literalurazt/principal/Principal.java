package com.aluracursos.literalurazt.principal;

import com.aluracursos.literalurazt.model.Autor;
import com.aluracursos.literalurazt.model.DatosAutor;
import com.aluracursos.literalurazt.model.DatosLibro;
import com.aluracursos.literalurazt.model.Libro;
import com.aluracursos.literalurazt.repository.AutorRepository;
import com.aluracursos.literalurazt.repository.LibroRepository;
import com.aluracursos.literalurazt.service.ConsumoAPI;
import com.aluracursos.literalurazt.service.ConvierteDatos;
import com.aluracursos.literalurazt.service.ConvierteDatosAutor;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private ConvierteDatosAutor conversorAutor = new ConvierteDatosAutor();
    private final String URL_BASE = "https://gutendex.com/books/";
    private Scanner teclado = new Scanner(System.in);
    private LibroRepository repository;
    private AutorRepository repository1;
    private List<Libro> libros;
    private List<Autor> autores;
    private Optional<Autor> autorBuscado;
    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    Elija la tarea a través de su número:
                    1- Buscar libro por título 
                    2- Lista de libros registrados
                    3- Lista de autores registrados
                    4- Lista de autores vivos en un determinado año
                    5- Lista de libros por idioma
                    6- buscar autores por nombre
                    7- Top 10 de libros mas descargados en la API
                    8- Top 5 libros en la DB
                    9- Autores con derecho de autor público                                  
                    0- Salir
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();
            switch (opcion) {
                case 1:
                    buscarLibroPorTitulo();
                    break;
                case 2:
                    mostrarLibrosBuscados();
                    break;
                case 3:
                    mostrarAutoresRegistrados();
                    break;
                case 4:
                    mostrarAutoresVivosPorAno();
                    break;
                case 5:
                    listarLibrosPorIdioma();
                    break;
                case 6:
                    buscarAutorPorNombre();
                    break;
                case 7:
                    top10LibrosAPI();
                    break;
                case 8:
                    top5LibrosDB();
                    break;
                case 9:
                    autoresConDerechoDeAutorPublico();
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }
        System.exit(0);
    }
    public Principal(LibroRepository repository, AutorRepository repository2) {
        this.repository = repository;
        this.repository1 = repository2;
    }
    private DatosLibro getDatosLibro(String nombreLibro) {
        var json = consumoAPI.obtenerDatos(URL_BASE + "?search="+ nombreLibro.replace(" ", "+"));
        DatosLibro datos = conversor.obtenerDatos(json, DatosLibro.class);
        return datos;
    }
    private DatosAutor getDatosAutor(String nombreLibro) {
        var json = consumoAPI.obtenerDatos(URL_BASE + "?search="+ nombreLibro.replace(" ", "+"));
        DatosAutor datos = conversorAutor.obtenerDatos(json, DatosAutor.class);
        return datos;
    }
    private String pregunta() {
        System.out.println("Escribe el nombre del libro que deseas buscar");
        var nombreLibro = teclado.nextLine();
        return nombreLibro;
    }
    private void mostrarLibrosBuscados(){
        try{
            List<Libro> libros = repository.findAll();
            libros.stream()
                    .sorted(Comparator.comparing(Libro::getDescargas))
                    .forEach(System.out::println);
        }catch(NullPointerException e){
            System.out.println(e.getMessage());
            libros = new ArrayList<>();
        }
    }

    public void buscarLibroPorTitulo() {
        mostrarLibrosBuscados();
        String libroBuscado = pregunta();

        libros = libros != null ? libros : new ArrayList<>();

        Optional<Libro> broli = libros.stream()
                .filter(l -> l.getTitulo().toLowerCase()
                        .contains(libroBuscado.toLowerCase()))
                .findFirst();

        if(broli.isPresent()) {
            var libroEncontrado = broli.get();
            System.out.println(libroEncontrado);
            System.out.println("El libro ya fue registrado en la DB, por favor intente con otro");
        }else{
            try {
                DatosLibro datosLibro = getDatosLibro(libroBuscado);
                System.out.println(datosLibro);

                if (datosLibro != null) {
                    DatosAutor datosAutor = getDatosAutor(libroBuscado);
                    if (datosAutor != null) {
                        List<Autor> autores = repository1.findAll();
                        autores = autores != null ? autores : new ArrayList<>();

                        Optional<Autor> pueta = autores.stream()
                                .filter(a -> datosAutor.nombre() != null &&
                                        a.getNombre().toLowerCase().contains(datosAutor.nombre().toLowerCase()))
                                .findFirst();

                        Autor autor;
                        if (pueta.isPresent()) {
                            autor = pueta.get();
                        } else {
                            autor = new Autor(
                                    datosAutor.nombre(),
                                    datosAutor.nacimiento(),
                                    datosAutor.deceso()
                            );
                            repository1.save(autor);
                        }

                        Libro libro = new Libro(
                                datosLibro.titulo(),
                                autor,
                                datosLibro.idioma() != null ? datosLibro.idioma() : Collections.emptyList(),
                                datosLibro.descargas()
                        );

                        libros.add(libro);
                        autor.setLibros(libros);

                        System.out.println(libro);
                        repository.save(libro);

                        System.out.println("Libro guardado exitosamente");
                    } else {
                        System.out.println("No se encontró el autor para el libro");
                    }

                } else {
                    System.out.println("No se encontró el libro");
                }
            } catch (Exception e) {
                System.out.println("excepción: " + e.getMessage());
            }
        }
    }

    public void mostrarAutoresRegistrados(){
        autores = repository1.findAll();
        autores.stream()
                .forEach(System.out::println);
    }

    public void mostrarAutoresVivosPorAno(){
        System.out.println("Ingrese un año");
        int anio = teclado.nextInt();
        autores = repository1.findAll();
        List<String> autoresNombre = autores.stream()
                .filter(a-> (a.getDeceso() >= anio) && (a.getNacimiento() <= anio))
                .map(a->a.getNombre())
                .collect(Collectors.toList());
        autoresNombre.forEach(System.out::println);
    }

    public void listarLibrosPorIdioma(){
        libros = repository.findAll();
        List<String> idiomasUnicos = libros.stream()
                .map(Libro::getIdioma)
                .distinct()
                .collect(Collectors.toList());
        idiomasUnicos.forEach(idioma -> {
            switch (idioma){
                case "en":
                    System.out.println("en - english");
                    break;
                case "es":
                    System.out.println("es - español");
                    break;
            }
        });
        System.out.println("");
        System.out.println("Ingrese el idioma en el que desea buscar los libros");
        String idiomaBuscado = teclado.nextLine();
        List<Libro> librosBuscados = libros.stream()
                .filter(l->l.getIdioma().contains(idiomaBuscado))
                .collect(Collectors.toList());
        librosBuscados.forEach(System.out::println);

    }

    public void buscarAutorPorNombre(){
        System.out.println("Ingrese el nombre del autor que desea buscar");
        var nombreAutor = teclado.nextLine();
        autorBuscado = repository1.findByNombreContainingIgnoreCase(nombreAutor);
        if(autorBuscado.isPresent()){
            System.out.println(autorBuscado.get());
        }else{
            System.out.println("Autor no encontrado");
        }
    }

    public void top10LibrosAPI() {
        try {
            String json = consumoAPI.obtenerDatos(URL_BASE + "?sort");

            List<DatosLibro> datosLibros = conversor.obtenerDatosArray(json, DatosLibro.class);
            List<DatosAutor> datosAutor = conversorAutor.obtenerDatosArray(json,DatosAutor.class);

            List<Libro> libros = new ArrayList<>();
            for (int i = 0; i < datosLibros.size(); i++) {
                Autor autor = new Autor(
                        datosAutor.get(i).nombre(),
                        datosAutor.get(i).nacimiento(),
                        datosAutor.get(i).deceso());

                Libro libro = new Libro(
                        datosLibros.get(i).titulo(),
                        autor,
                        datosLibros.get(i).idioma(),
                        datosLibros.get(i).descargas());
                libros.add(libro);
            }

            libros.sort(Comparator.comparingDouble(Libro::getDescargas).reversed());

            List<Libro> top10Libros = libros.subList(0, Math.min(10, libros.size()));

            for (int i = 0; i < top10Libros.size(); i++) {
                System.out.println((i + 1) + ". " + top10Libros.get(i));
            }

        } catch (NullPointerException e) {
            System.out.println("Error occurred: " + e.getMessage());
        }
    }

    public void top5LibrosDB(){
        try{
            List<Libro> libros = repository.findAll();
            List<Libro> librosOrdenados = libros.stream()
                    .sorted(Comparator.comparingDouble(Libro::getDescargas).reversed())
                    .collect(Collectors.toList());
            List<Libro> top5Libros = librosOrdenados.subList(0, Math.min(5, librosOrdenados.size()));
            for (int i = 0; i < top5Libros.size(); i++) {
                System.out.println((i + 1) + ". " + top5Libros.get(i));
            }
        }catch(NullPointerException e){
            System.out.println(e.getMessage());
            libros = new ArrayList<>();
        }
    }

    public void autoresConDerechoDeAutorPublico(){
        try {
            String json = consumoAPI.obtenerDatos(URL_BASE + "?sort");

            List<DatosAutor> datosAutor = conversorAutor.obtenerDatosArray(json, DatosAutor.class);

            Map<String, Autor> autoresMap = new HashMap<>();

            for (DatosAutor datoAutor : datosAutor) {
                String nombre = datoAutor.nombre();
                Autor autor = autoresMap.get(nombre);

                if (autor == null) {
                    autor = new Autor(nombre, datoAutor.nacimiento(), datoAutor.deceso());
                    autoresMap.put(nombre, autor);
                }

                List<Libro> librosArray = new ArrayList<>();
                autor.setLibros(librosArray);
            }

            List<Autor> autoresOrdenados = autoresMap.values().stream()
                    .filter(a -> a.getDeceso() < 1954)
                    .collect(Collectors.toList());

            List<Autor> diezAutores = autoresOrdenados.subList(0, Math.min(10, autoresOrdenados.size()));

            for (int i = 0; i < diezAutores.size(); i++) {
                System.out.println((i + 1) + ". " + diezAutores.get(i).getNombre()+"/n"+
                        ", año de fallecimiento: "+diezAutores.get(i).getDeceso());
            }

        } catch (NullPointerException e) {
            System.out.println("Error occurred: " + e.getMessage());
        }
    }

}
