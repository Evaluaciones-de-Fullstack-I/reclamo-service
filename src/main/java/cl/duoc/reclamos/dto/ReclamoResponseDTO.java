package cl.duoc.reclamos.dto;

import cl.duoc.reclamos.model.EstadoReclamo;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReclamoResponseDTO {
    private Long id;
    private Long clienteId;
    private Long pedidoId;
    private String motivo;
    private String descripcion;
    private EstadoReclamo estado;
    private String resolucionAdmin;
    private LocalDateTime fechaCreacion;
}