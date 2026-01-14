package rebelsrescue.fleet.spi.stubs;

import ddd.Stub;
import rebelsrescue.fleet.Fleet;
import rebelsrescue.fleet.spi.Fleets;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Stub
public class InMemoryFleets implements Fleets {
    private final Map<UUID, Fleet> fleets = new HashMap<>();

    @Override
    public Fleet getById(UUID id) {
        return fleets.get(id);
    }

    @Override
    public Fleet save(Fleet fleet) {
        fleets.put(fleet.id(), fleet);
        return fleet;
    }
}
