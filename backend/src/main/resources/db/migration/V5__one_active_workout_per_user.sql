CREATE UNIQUE INDEX uq_trening_active_per_user ON trening(id_korisnika) WHERE status = 'IN_PROGRESS';
