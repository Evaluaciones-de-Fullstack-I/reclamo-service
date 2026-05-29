package cl.duoc.reclamos.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reclamos")
public class Reclamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardamos los IDs referenciales de los otros microservicios
    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReclamo estado;

    @Column(length = 500)
    private String resolucionAdmin;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoReclamo.PENDIENTE;
        }
    }
}