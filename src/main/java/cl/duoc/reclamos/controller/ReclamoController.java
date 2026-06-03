package cl.duoc.reclamos.controller;

import cl.duoc.reclamos.dto.ReclamoRequestDTO;
import cl.duoc.reclamos.dto.ReclamoResponseDTO;
import cl.duoc.reclamos.model.EstadoReclamo;
import cl.duoc.reclamos.model.Reclamo;
import cl.duoc.reclamos.repository.ReclamoRepository;
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
public class ReclamoController {

    @Autowired
    private ReclamoRepository reclamoRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    // Cliente crea un reclamo
    @PostMapping
    public ResponseEntity<ReclamoResponseDTO> crearReclamo(@Valid @RequestBody ReclamoRequestDTO dto) {
        Reclamo reclamo = new Reclamo();
        reclamo.setClienteId(dto.getClienteId());
        reclamo.setPedidoId(dto.getPedidoId());
        reclamo.setMotivo(dto.getMotivo());
        reclamo.setDescripcion(dto.getDescripcion());
        
        Reclamo guardado = reclamoRepository.save(reclamo);
        return new ResponseEntity<>(convertirADto(guardado), HttpStatus.CREATED);
    }

    // Administrador revisa reclamos pendientes
    @GetMapping("/pendientes")
    public ResponseEntity<List<ReclamoResponseDTO>> obtenerPendientes() {
        List<Reclamo> reclamos = reclamoRepository.findByEstado(EstadoReclamo.PENDIENTE);
        List<ReclamoResponseDTO> response = reclamos.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // Administrador actualiza el estado y autoriza reembolso (Con WebClient)
    @PutMapping("/{id}/resolver")
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

        // 💰 [CONEXIÓN AUTOMÁTICA] SI EL ADMINISTRADOR CONCEDE EL REEMBOLSO, SE NOTIFICA AL MS DE PAGOS
        if (nuevoEstado == EstadoReclamo.REEMBOLSO_AUTORIZADO) {
            try {
                // Modificado para apuntar exactamente al puerto 8088 de tu MS Pagos
                webClientBuilder.build().put()
                        .uri("http://localhost:8088/api/pagos/reembolsar/pedido/" + actualizado.getPedidoId())
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block(); // Cambiado a .block() para asegurar la transacción sincrónica

                System.out.println("💰 [CONEXIÓN] Solicitud de reembolso del Pedido ID " + actualizado.getPedidoId() + " procesada exitosamente en MS Pagos.");
                
            } catch (Exception e) {
                System.out.println("❌ [ERROR] No se pudo procesar el reembolso en el MS Pagos: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(convertirADto(actualizado));
    }

    // Método auxiliar para mapeo rápido
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