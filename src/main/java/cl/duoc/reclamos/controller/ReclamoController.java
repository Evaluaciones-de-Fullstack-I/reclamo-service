package cl.duoc.reclamos.controller;

import cl.duoc.reclamos.dto.ReclamoRequestDTO;
import cl.duoc.reclamos.dto.ReclamoResponseDTO;
import cl.duoc.reclamos.model.EstadoReclamo;
import cl.duoc.reclamos.model.Reclamo;
import cl.duoc.reclamos.repository.ReclamoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reclamos")
@Tag(name = "Reclamos", description = "Controlador para la gestión de reclamos de clientes y reembolsos coordinados por WebClient")
public class ReclamoController {

    @Autowired
    private ReclamoRepository reclamoRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping
    @Operation(
        summary = "Crear un nuevo reclamo",
        description = "Permite a un cliente registrar un reclamo asociado a un pedido específico por algún motivo formal.",
        responses = {
            @ApiResponse(
                responseCode = "201", 
                description = "Reclamo creado exitosamente"
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "Datos de entrada inválidos"
            )
        }
    )
    public ResponseEntity<ReclamoResponseDTO> crearReclamo(
            @Valid @RequestBody(
                description = "Estructura JSON necesaria para interponer un nuevo reclamo",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReclamoRequestDTO.class),
                    examples = @ExampleObject(
                        name = "Ejemplo de Reclamo",
                        value = "{\n  \"clienteId\": 45,\n  \"pedidoId\": 10024,\n  \"motivo\": \"Producto defectuoso o dañado\",\n  \"descripcion\": \"La pantalla del monitor llegó trizada en la esquina inferior izquierda.\"\n}"
                    )
                )
            )
            @org.springframework.web.bind.annotation.RequestBody ReclamoRequestDTO dto) {
        
        Reclamo reclamo = new Reclamo();
        reclamo.setClienteId(dto.getClienteId());
        reclamo.setPedidoId(dto.getPedidoId());
        reclamo.setMotivo(dto.getMotivo());
        reclamo.setDescripcion(dto.getDescripcion());
        
        Reclamo guardado = reclamoRepository.save(reclamo);
        return new ResponseEntity<>(convertirADto(guardado), HttpStatus.CREATED);
    }

    @GetMapping("/pendientes")
    @Operation(
        summary = "Obtener reclamos pendientes",
        description = "Retorna una lista con todos los reclamos cuyo estado actual es PENDIENTE para la revisión del administrador.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Lista de reclamos pendientes obtenida con éxito"
            )
        }
    )
    public ResponseEntity<List<ReclamoResponseDTO>> obtenerPendientes() {
        List<Reclamo> reclamos = reclamoRepository.findByEstado(EstadoReclamo.PENDIENTE);
        List<ReclamoResponseDTO> response = reclamos.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/resolver")
    @Operation(
        summary = "Resolver un reclamo",
        description = "Permite al administrador cambiar el estado del reclamo y, en caso de autorizar un reembolso, se comunica síncronamente con el MS de Pagos (puerto 8088).",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Reclamo resuelto y actualizado exitosamente"
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "No se encontró el reclamo con el ID proporcionado"
            )
        }
    )
    public ResponseEntity<ReclamoResponseDTO> resolverReclamo(
            @PathVariable Long id, 
            @RequestParam EstadoReclamo nuevoEstado, 
            @RequestParam(required = false) String resolucion) {
        
        Reclamo reclamo = reclamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamo no encontrado"));

        reclamo.setEstado(nuevoEstado);
        if (resolucion != null) {
            reclamo.setResolucionAdmin(resolucion);
        }

        Reclamo actualizado = reclamoRepository.save(reclamo);

        if (nuevoEstado == EstadoReclamo.REEMBOLSO_AUTORIZADO) {
            try {
                webClientBuilder.build().put()
                        .uri("http://localhost:8088/api/pagos/reembolsar/pedido/" + actualizado.getPedidoId())
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();

                System.out.println("💰 [CONEXIÓN] Solicitud de reembolso del Pedido ID " + actualizado.getPedidoId() + " procesada exitosamente en MS Pagos.");
                
            } catch (Exception e) {
                System.out.println("❌ [ERROR] No se pudo procesar el reembolso en el MS Pagos: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(convertirADto(actualizado));
    }

    private ReclamoResponseDTO convertirADto(Reclamo reclamo) {
        ReclamoResponseDTO dto = new ReclamoResponseDTO();
        dto.setId(reclamo.getId());
        dto.setClienteId(reclamo.getClienteId());
        dto.setPedidoId(reclamo.getPedidoId());
        dto.setMotivo(reclamo.getMotivo());
        dto.setDescripcion(reclamo.getDescripcion());
        dto.setEstado(reclamo.getEstado());
        dto.setResolucionAdmin(reclamo.getResolucionAdmin());
        dto.setFechaCreacion(reclamo.getFechaCreacion());
        return dto;
    }
}