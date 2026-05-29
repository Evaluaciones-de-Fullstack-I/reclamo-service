package cl.duoc.reclamos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReclamoRequestDTO {
    
    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "El ID del pedido es obligatorio")
    private Long pedidoId;

    @NotBlank(message = "El motivo no puede estar vacío")
    private String motivo;

    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;
}