package com.fitpulse.backend.template;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.exercise.Vezba;
import com.fitpulse.backend.exercise.VezbaRepository;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.template.dto.TemplateRequest;
import com.fitpulse.backend.template.dto.TemplateResponse;
import com.fitpulse.backend.template.dto.TemplateVezbaRequest;
import com.fitpulse.backend.template.dto.TemplateVezbaResponse;
import com.fitpulse.backend.user.KorisnikRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final VezbaRepository vezbaRepository;
    private final KorisnikRepository korisnikRepository;
    private final OwnershipGuard ownershipGuard;

    public TemplateService(TemplateRepository templateRepository,
                            VezbaRepository vezbaRepository,
                            KorisnikRepository korisnikRepository,
                            OwnershipGuard ownershipGuard) {
        this.templateRepository = templateRepository;
        this.vezbaRepository = vezbaRepository;
        this.korisnikRepository = korisnikRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> findAll(CustomUserDetails principal) {
        return templateRepository.findVisible(ownershipGuard.viewerIdForQuery(principal)).stream()
                .map(TemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse getById(Long id, CustomUserDetails principal) {
        return TemplateResponse.from(findVisibleOrThrow(id, principal));
    }

    @Transactional
    public TemplateResponse create(TemplateRequest request, CustomUserDetails principal) {
        Long ownerId = ownershipGuard.resolveOwnerIdForCreate(request.system(), principal);
        Template template = Template.create(
                request.naziv(),
                request.opis(),
                ownerId != null ? korisnikRepository.getReferenceById(ownerId) : null);

        if (request.exercises() != null) {
            request.exercises().forEach(item -> addItem(template, item));
        }

        return TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public TemplateResponse update(Long id, TemplateRequest request, CustomUserDetails principal) {
        Template template = findModifiableOrThrow(id, principal);

        template.setNaziv(request.naziv());
        template.setOpis(request.opis());

        if (request.exercises() != null) {
            template.clearExercises();
            templateRepository.flush(); // stari redovi moraju da se obrišu pre inserta, zbog UNIQUE(id_template, redni_broj)
            request.exercises().forEach(item -> addItem(template, item));
            templateRepository.flush();
        }

        return TemplateResponse.from(template);
    }

    @Transactional
    public void delete(Long id, CustomUserDetails principal) {
        Template template = findModifiableOrThrow(id, principal);
        templateRepository.delete(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateVezbaResponse> getExercises(Long id, CustomUserDetails principal) {
        return findVisibleOrThrow(id, principal).getExercises().stream()
                .map(TemplateVezbaResponse::from)
                .toList();
    }

    @Transactional
    public TemplateVezbaResponse addExercise(Long id, TemplateVezbaRequest request, CustomUserDetails principal) {
        Template template = findModifiableOrThrow(id, principal);
        TemplateVezba item = addItem(template, request);
        templateRepository.flush(); // da novi red dobije id pre mapiranja u response
        return TemplateVezbaResponse.from(item);
    }

    @Transactional
    public TemplateVezbaResponse updateExercise(Long id, Long itemId, TemplateVezbaRequest request,
                                                CustomUserDetails principal) {
        Template template = findModifiableOrThrow(id, principal);
        TemplateVezba item = findItemOrThrow(template, itemId);

        item.setVezba(resolveVezba(request.vezbaId(), template.getOwnerId()));
        item.setBrojSerija(request.brojSerija());
        item.setBrojPonavljanja(request.brojPonavljanja());
        item.setKilaza(request.kilaza());

        return TemplateVezbaResponse.from(item);
    }

    @Transactional
    public void removeExercise(Long id, Long itemId, CustomUserDetails principal) {
        Template template = findModifiableOrThrow(id, principal);
        template.removeExercise(findItemOrThrow(template, itemId));
    }

    private TemplateVezba addItem(Template template, TemplateVezbaRequest item) {
        Vezba vezba = resolveVezba(item.vezbaId(), template.getOwnerId());
        return template.addExercise(vezba, item.brojSerija(), item.brojPonavljanja(), item.kilaza());
    }

    // u template sme samo sistemska vežba ili vežba vlasnika template-a
    private Vezba resolveVezba(Long vezbaId, Long templateOwnerId) {
        return vezbaRepository.findById(vezbaId)
                .filter(vezba -> vezba.getOwnerId() == null || vezba.getOwnerId().equals(templateOwnerId))
                .orElseThrow(() -> ApiException.badRequest("Vežba " + vezbaId + " nije dostupna"));
    }

    private Template findVisibleOrThrow(Long id, CustomUserDetails principal) {
        return templateRepository.findWithExercisesById(id)
                .filter(template -> ownershipGuard.canView(template.getOwnerId(), principal))
                .orElseThrow(() -> ApiException.notFound("Template nije pronađen"));
    }

    private Template findModifiableOrThrow(Long id, CustomUserDetails principal) {
        Template template = findVisibleOrThrow(id, principal);
        ownershipGuard.assertCanModify(template.getOwnerId(), principal);
        return template;
    }

    private TemplateVezba findItemOrThrow(Template template, Long itemId) {
        return template.getExercises().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Vežba nije pronađena u template-u"));
    }
}
