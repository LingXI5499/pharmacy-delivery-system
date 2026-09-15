package com.pharmacy.inventory;

import com.pharmacy.entity.PharmacyOrderItem;
import com.pharmacy.mapper.MedicineMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventoryServiceImplTest {
    @Test
    void reserveSplitsAcrossBatchesInMapperFefoOrder() {
        MedicineBatchMapper batches=mock(MedicineBatchMapper.class);
        InventoryReservationMapper reservations=mock(InventoryReservationMapper.class);
        InventoryLedgerMapper ledger=mock(InventoryLedgerMapper.class);
        MedicineMapper medicines=mock(MedicineMapper.class);
        InventoryServiceImpl service=new InventoryServiceImpl(batches,reservations,ledger,medicines);
        MedicineBatch first=batch(11L,3),second=batch(12L,5);
        when(batches.selectSellableForUpdate(eq(9L),any(LocalDate.class))).thenReturn(List.of(first,second));
        when(batches.reserve(anyLong(),anyInt())).thenReturn(1);
        when(medicines.decreaseStock(eq(9L),anyInt())).thenReturn(1);
        PharmacyOrderItem item=new PharmacyOrderItem();item.setId(20L);item.setMedicineId(9L);item.setMedicineName("虚构测试药品");item.setQuantity(7);

        service.reserve(30L,List.of(item),LocalDateTime.now().plusMinutes(30),7L);

        verify(batches).reserve(11L,3);verify(batches).reserve(12L,4);
        ArgumentCaptor<InventoryReservation> captor=ArgumentCaptor.forClass(InventoryReservation.class);
        verify(reservations,times(2)).insert(captor.capture());
        assertEquals(List.of(3,4),captor.getAllValues().stream().map(InventoryReservation::getQuantity).toList());
        verify(ledger,times(2)).insert(any(InventoryLedger.class));
    }
    private MedicineBatch batch(Long id,int available){MedicineBatch b=new MedicineBatch();b.setId(id);b.setMedicineId(9L);b.setAvailableQty(available);b.setReservedQty(0);return b;}
}
