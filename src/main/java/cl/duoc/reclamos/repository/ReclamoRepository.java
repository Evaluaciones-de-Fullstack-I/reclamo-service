package cl.duoc.reclamos.repository;

import cl.duoc.reclamos.model.EstadoReclamo;
import cl.duoc.reclamos.model.Reclamo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReclamoRepository extends JpaRepository<Reclamo, Long> {
    List<Reclamo> findByClienteId(Long clienteId);
    List<Reclamo> findByEstado(EstadoReclamo estado);
}