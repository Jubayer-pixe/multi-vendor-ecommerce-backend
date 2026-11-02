
// package com.abc.SpringBootSecqurityEx.service;

// import com.abc.SpringBootSecqurityEx.dtos.DeliveryDTO;
// import com.abc.SpringBootSecqurityEx.entity.delivery.Delivery;
// import com.abc.SpringBootSecqurityEx.entity.delivery.DeliveryAgent;
// import com.abc.SpringBootSecqurityEx.entity.delivery.DeliveryStatus;
// import com.abc.SpringBootSecqurityEx.entity.order.Order;
// import com.abc.SpringBootSecqurityEx.repository.DeliveryAgentRepository;
// import com.abc.SpringBootSecqurityEx.repository.DeliveryRepository;
// import com.abc.SpringBootSecqurityEx.repository.OrderRepository;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;

// @Service
// @Transactional
// public class DeliveryService {

//     private final DeliveryRepository deliveryRepository;
//     private final OrderRepository orderRepository;
//     private final DeliveryAgentRepository agentRepository;

//     public DeliveryService(
//             DeliveryRepository deliveryRepository,
//             OrderRepository orderRepository,
//             DeliveryAgentRepository agentRepository) {
//         this.deliveryRepository = deliveryRepository;
//         this.orderRepository = orderRepository;
//         this.agentRepository = agentRepository;
//     }

//     public DeliveryDTO create(DeliveryDTO dto) {
//         if (dto.getOrderId() == null ||
//                 dto.getDeliveryAgentId() == null) {
//             throw new IllegalArgumentException(
//                     "Order ID and delivery agent ID are required");
//         }

//         Order order = orderRepository.findById(dto.getOrderId())
//                 .orElseThrow(() -> new IllegalArgumentException("Order not found"));

//         DeliveryAgent agent = agentRepository.findById(
//                 dto.getDeliveryAgentId()).orElseThrow(() -> new IllegalArgumentException("Delivery agent not found"));

//         if (deliveryRepository.findAll().stream()
//                 .anyMatch(d -> d.getOrder().getId().equals(order.getId()))) {
//             throw new IllegalArgumentException(
//                     "A delivery already exists for this order");
//         }

//         if (!Boolean.TRUE.equals(agent.getActive())) {
//             throw new IllegalArgumentException(
//                     "Delivery agent is inactive");
//         }

//         Delivery entity = new Delivery();
//         entity.setOrder(order);
//         entity.setDeliveryAgent(agent);
//         entity.setDeliveryAddress(
//                 dto.getDeliveryAddress() == null ||
//                         dto.getDeliveryAddress().isBlank()
//                                 ? order.getShippingAddress()
//                                 : dto.getDeliveryAddress().trim());
//         entity.setTrackingNote(dto.getTrackingNote());
//         entity.setStatus(DeliveryStatus.ASSIGNED);

//         return toDto(deliveryRepository.save(entity));
//     }

//     public DeliveryDTO update(Long id, DeliveryDTO dto) {
//         Delivery entity = deliveryRepository.findById(id)
//                 .orElseThrow(() -> new IllegalArgumentException("Delivery not found"));

//         if (dto.getDeliveryAddress() != null &&
//                 !dto.getDeliveryAddress().isBlank()) {
//             entity.setDeliveryAddress(dto.getDeliveryAddress().trim());
//         }

//         entity.setTrackingNote(dto.getTrackingNote());

//         if (dto.getStatus() != null) {
//             changeStatus(entity, dto.getStatus());
//         }

//         return toDto(deliveryRepository.save(entity));
//     }

//     private void changeStatus(
//             Delivery entity,
//             DeliveryStatus newStatus) {
//         DeliveryStatus oldStatus = entity.getStatus();

//         if (oldStatus == DeliveryStatus.DELIVERED ||
//                 oldStatus == DeliveryStatus.CANCELLED ||
//                 oldStatus == DeliveryStatus.FAILED) {
//             throw new IllegalArgumentException(
//                     "This delivery is already in a terminal state");
//         }

//         if (newStatus == DeliveryStatus.PICKED_UP) {
//             entity.setPickedUpAt(LocalDateTime.now());
//         }

//         if (newStatus == DeliveryStatus.DELIVERED) {
//             if (entity.getPickedUpAt() == null) {
//                 throw new IllegalArgumentException(
//                         "Pickup must be recorded before delivery");
//             }

//             entity.setDeliveredAt(LocalDateTime.now());
//         }

//         entity.setStatus(newStatus);
//     }

//     @Transactional(readOnly = true)
//     public List<DeliveryDTO> findAll() {
//         return deliveryRepository.findAll()
//                 .stream()
//                 .map(this::toDto)
//                 .toList();
//     }

//     @Transactional(readOnly = true)
//     public List<DeliveryDTO> findByAgent(Long agentId) {
//         return deliveryRepository.findByDeliveryAgent_Id(agentId)
//                 .stream()
//                 .map(this::toDto)
//                 .toList();
//     }

//     @Transactional(readOnly = true)
//     public Optional<DeliveryDTO> findById(Long id) {
//         return deliveryRepository.findById(id).map(this::toDto);
//     }

//     public void deleteById(Long id) {
//         if (!deliveryRepository.existsById(id)) {
//             throw new IllegalArgumentException("Delivery not found");
//         }

//         deliveryRepository.deleteById(id);
//     }

//     private DeliveryDTO toDto(Delivery entity) {
//         DeliveryDTO dto = new DeliveryDTO();

//         dto.setId(entity.getId());
//         dto.setOrderId(entity.getOrder().getId());
//         dto.setOrderNumber(entity.getOrder().getOrderNumber());
//         dto.setDeliveryAgentId(entity.getDeliveryAgent().getId());
//         dto.setDeliveryAgentName(entity.getDeliveryAgent().getFullName());
//         dto.setStatus(entity.getStatus());
//         dto.setDeliveryAddress(entity.getDeliveryAddress());
//         dto.setTrackingNote(entity.getTrackingNote());
//         dto.setAssignedAt(entity.getAssignedAt());
//         dto.setPickedUpAt(entity.getPickedUpAt());
//         dto.setDeliveredAt(entity.getDeliveredAt());
//         dto.setCreatedAt(entity.getCreatedAt());
//         dto.setUpdatedAt(entity.getUpdatedAt());

//         return dto;
//     }
// }
