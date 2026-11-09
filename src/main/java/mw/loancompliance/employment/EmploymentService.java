package mw.loancompliance.employment;
import mw.loancompliance.organization.OrganizationRepository;
import mw.loancompliance.student.StudentRepository;
import mw.loancompliance.user.repository.UserRepository;
import mw.loancompliance.audit.AuditService;
import mw.loancompliance.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
@Service @Transactional
public class EmploymentService {
    private final EmploymentRecordRepository records;
    private final StudentRepository students;
    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final AuditService audit;
    private final NotificationService notifications;
    public EmploymentService(EmploymentRecordRepository r,StudentRepository s,OrganizationRepository o,UserRepository u,AuditService a,NotificationService n) {
        records=r;
        students=s;
        organizations=o;
        users=u;
        audit=a;
        notifications=n;
    }
    public EmploymentRecord submit(String email,String type,String category,Long organizationId,String employer,String title,LocalDate start) {
        if(type==null||category==null||start==null) throw bad("Employment type, category and start date are required");
        boolean self="SELF_EMPLOYED".equals(category), unregistered="UNREGISTERED_EMPLOYER".equals(category);
        if(self && organizationId!=null || unregistered && (organizationId!=null||employer==null||employer.isBlank()) || !self&&!unregistered&&organizationId==null) throw bad("Provide a registered organization, self-employment declaration, or unregistered employer details");
        var student=students.findByUser_EmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student profile not found"));
        var record=new EmploymentRecord();
        record.setStudent(student);
        record.setEmploymentType(type);
        record.setEmploymentCategory(category);
        record.setEmployerName(employer);
        record.setJobTitle(title);
        record.setStartDate(start);
        if(organizationId!=null) record.setOrganization(organizations.findById(organizationId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Organization not found")));
        record.setStatus(self?"VERIFIED":unregistered?"REQUIRES_REVIEW":"PENDING_VERIFICATION");
        var saved=records.save(record);
        audit.record(email,"EMPLOYMENT_SUBMITTED","EMPLOYMENT_RECORD",saved.getId(),"Student submitted employment information");
        if(saved.getOrganization()!=null)users.findAll().stream().filter(u->"EMPLOYER_ADMIN".equals(u.getRole())&&saved.getOrganization().getId().equals(u.getOrganizationId())).forEach(u->notifications.inApp(u,"EMPLOYMENT_SUBMITTED","Employment verification requested","A student submitted employment information for your organization."));
        return saved;
    }
    @Transactional(readOnly=true) public List<EmploymentRecord> own(String email) {
        return records.findAllByStudent_User_EmailIgnoreCaseOrderByStartDateDesc(email);
    }
    @Transactional(readOnly=true) public List<EmploymentRecord> all() {
        return records.findAllByOrderByStartDateDesc();
    }
    @Transactional(readOnly=true) public EmploymentRecord get(Long id,String email,boolean admin) {
        var r=find(id);
        if(!admin&&(r.getOrganization()==null||!users.findByEmailIgnoreCase(email).map(u->r.getOrganization().getId().equals(u.getOrganizationId())).orElse(false)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Record belongs to another organization");
        return r;
    }
    @Transactional(readOnly=true) public List<EmploymentRecord> pending(String email) {
        var u=users.findByEmailIgnoreCase(email).orElseThrow();
        if(u.getOrganizationId()==null) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No organization assigned");
        return records.findAllByOrganization_IdAndStatusOrderByStartDateAsc(u.getOrganizationId(),"PENDING_VERIFICATION");
    }
    public EmploymentRecord decide(Long id,String email,boolean admin,String status,String note) {
        var r=find(id);
        if(!admin && (r.getOrganization()==null||!organizations.findById(r.getOrganization().getId()).map(o->users.findByEmailIgnoreCase(email).map(u->r.getOrganization().getId().equals(u.getOrganizationId())).orElse(false)).orElse(false))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Record belongs to another organization");
        if(!List.of("VERIFIED","REJECTED","REQUIRES_REVIEW").contains(status))throw bad("Invalid employment review status");
        if(!"PENDING_VERIFICATION".equals(r.getStatus())&& !"REQUIRES_REVIEW".equals(r.getStatus())) throw bad("Employment record is already decided");
        r.setStatus(status);
        r.setVerificationNote(note);
        audit.record(email,"EMPLOYMENT_"+status,"EMPLOYMENT_RECORD",id,"Employment verification decision: "+status);
        notifications.inApp(r.getStudent().getUser(),"EMPLOYMENT_"+status,"Employment status updated","Your employment record was reviewed: "+status);
        return r;
    }
    public EmploymentRecord updateOwn(Long id,String email,String title,LocalDate start) {
        var r=records.findByIdAndStudent_User_EmailIgnoreCase(id,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Employment record not found"));
        if(title!=null)r.setJobTitle(title);
        if(start!=null)r.setStartDate(start);
        if(title==null&&start==null)throw bad("At least one field is required");
        if(r.getOrganization()!=null)r.setStatus("PENDING_VERIFICATION");
        return r;
    }
    public EmploymentRecord end(Long id,String email,boolean employer,LocalDate date) {
        var r=find(id);
        if(date==null||date.isBefore(r.getStartDate()))throw bad("End date must not precede start date");
        if(!employer&&!r.getStudent().getUser().getEmail().equalsIgnoreCase(email))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your employment record");
        if(employer&&(r.getOrganization()==null||!users.findByEmailIgnoreCase(email).map(u->r.getOrganization().getId().equals(u.getOrganizationId())).orElse(false)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Record belongs to another organization");
        r.setEndDate(date);
        r.setStatus("ENDED");
        return r;
    }
    private EmploymentRecord find(Long id) {
        return records.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Employment record not found"));
    }
    private ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);
    }
}
