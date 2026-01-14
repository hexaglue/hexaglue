package rebelsrescue.fleet.spi;

import rebelsrescue.fleet.Fleet;

import java.util.UUID;

public interface Fleets {
    Fleet getById(UUID id);

    Fleet save(Fleet fleet);
}
