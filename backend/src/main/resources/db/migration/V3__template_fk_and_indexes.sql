ALTER TABLE trening DROP CONSTRAINT trening_id_template_fkey;
ALTER TABLE trening ADD CONSTRAINT trening_id_template_fkey
    FOREIGN KEY (id_template) REFERENCES template(id) ON DELETE SET NULL;

CREATE INDEX idx_vezba_created_by ON vezba(created_by);
CREATE INDEX idx_template_created_by ON template(created_by);
CREATE INDEX idx_template_vezba_vezba ON template_vezba(id_vezbe);
CREATE INDEX idx_trening_korisnik ON trening(id_korisnika);
CREATE INDEX idx_trening_template ON trening(id_template);
CREATE INDEX idx_trening_vezba_trening ON trening_vezba(id_treninga);
CREATE INDEX idx_trening_vezba_vezba ON trening_vezba(id_vezbe);
CREATE INDEX idx_trening_serija_trening_vezba ON trening_serija(id_trening_vezbe);
CREATE INDEX idx_licni_rekord_vezba ON licni_rekord(id_vezbe);
