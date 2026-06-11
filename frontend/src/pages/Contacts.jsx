import { useState, useEffect, useCallback } from 'react';
import { Plus, X, Phone, Mail, MapPin, Edit2, Trash2, Search, Filter } from 'lucide-react';
import Card from '../components/ui/Card';
import { debtService } from '../services/api';
import './Contacts.css';

const emptyContactForm = () => ({
  fullName: '',
  phone: '',
  email: '',
  address: '',
  contactType: 'BOTH',
  notes: ''
});

const Contacts = () => {
  const [contacts, setContacts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState('ALL');

  const [showModal, setShowModal] = useState(false);
  const [editingContact, setEditingContact] = useState(null);
  const [form, setForm] = useState(emptyContactForm());

  const loadContacts = useCallback(async () => {
    try {
      setIsLoading(true);
      setError('');
      const res = await debtService.getContacts();
      setContacts(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load contacts.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadContacts();
  }, [loadContacts]);

  const handleOpenCreate = () => {
    setEditingContact(null);
    setForm(emptyContactForm());
    setShowModal(true);
  };

  const handleOpenEdit = (contact) => {
    setEditingContact(contact);
    setForm({
      fullName: contact.fullName || '',
      phone: contact.phone || '',
      email: contact.email || '',
      address: contact.address || '',
      contactType: contact.contactType || 'BOTH',
      notes: contact.notes || ''
    });
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setIsSaving(true);
      setError('');
      if (editingContact) {
        await debtService.updateContact(editingContact.id, form);
      } else {
        await debtService.createContact(form);
      }
      setShowModal(false);
      setForm(emptyContactForm());
      await loadContacts();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save contact.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this contact? This might affect their linked debt records.")) return;
    try {
      await debtService.deleteContact(id);
      await loadContacts();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete contact.');
    }
  };

  const filteredContacts = contacts.filter(c => {
    const matchesSearch = c.fullName.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesType = filterType === 'ALL' || c.contactType === filterType || c.contactType === 'BOTH';
    return matchesSearch && matchesType;
  });

  return (
    <div className="contacts-portal">
      <div className="portal-header">
        <div>
          <h2 className="portal-title">Contacts & Entities</h2>
          <p className="portal-subtitle">Manage the people and businesses in your financial network.</p>
        </div>
        <div className="header-actions">
          <button type="button" className="btn-primary" onClick={handleOpenCreate}>
            <Plus size={18} /> New Contact
          </button>
        </div>
      </div>

      {error && <div className="portal-error">{error}</div>}

      <div className="contacts-controls">
        <div style={{ position: 'relative', flex: 1, maxWidth: '400px' }}>
          <Search size={18} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }} />
          <input 
            type="text" 
            className="search-input" 
            placeholder="Search contacts by name..." 
            style={{ paddingLeft: '2.5rem', width: '100%' }}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <select 
          className="filter-select"
          value={filterType}
          onChange={(e) => setFilterType(e.target.value)}
        >
          <option value="ALL">All Contact Types</option>
          <option value="DEBTOR">Debtors Only</option>
          <option value="CREDITOR">Creditors Only</option>
        </select>
      </div>

      {isLoading ? (
        <div className="empty-state">Loading contacts...</div>
      ) : filteredContacts.length === 0 ? (
        <div className="empty-state">
          {contacts.length === 0 
            ? "Your address book is empty. Add a new contact to get started." 
            : "No contacts match your current search/filter criteria."}
        </div>
      ) : (
        <div className="contacts-grid">
          {filteredContacts.map(contact => (
            <Card key={contact.id} className="contact-card">
              <div className="contact-header">
                <h3 className="contact-name">{contact.fullName}</h3>
                <span className={`contact-type-badge ${contact.contactType.toLowerCase()}`}>
                  {contact.contactType}
                </span>
              </div>
              
              {contact.phone && (
                <div className="contact-info-row">
                  <Phone size={14} /> {contact.phone}
                </div>
              )}
              {contact.email && (
                <div className="contact-info-row">
                  <Mail size={14} /> {contact.email}
                </div>
              )}
              {contact.address && (
                <div className="contact-info-row">
                  <MapPin size={14} /> {contact.address}
                </div>
              )}
              
              {contact.notes && (
                <div className="contact-notes">
                  "{contact.notes}"
                </div>
              )}

              <div className="contact-actions">
                <button type="button" className="action-icon-btn" onClick={() => handleOpenEdit(contact)} title="Edit Contact">
                  <Edit2 size={16} />
                </button>
                <button type="button" className="action-icon-btn delete" onClick={() => handleDelete(contact.id)} title="Delete Contact">
                  <Trash2 size={16} />
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {showModal && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>{editingContact ? 'Edit Contact' : 'Create New Contact'}</h3>
              <button type="button" className="icon-close" onClick={() => setShowModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleSubmit} className="modal-form">
              <div className="form-group">
                <label>Full Name or Entity Name</label>
                <input 
                  value={form.fullName} 
                  onChange={(e) => setForm({...form, fullName: e.target.value})} 
                  placeholder="e.g. John Doe or Acme Corp" 
                  required 
                />
              </div>
              
              <div style={{ display: 'flex', gap: '1rem' }}>
                <div className="form-group" style={{ flex: 1 }}>
                  <label>Phone Number (Optional)</label>
                  <input 
                    value={form.phone} 
                    onChange={(e) => setForm({...form, phone: e.target.value})} 
                    placeholder="+123456789" 
                  />
                </div>
                <div className="form-group" style={{ flex: 1 }}>
                  <label>Email Address (Optional)</label>
                  <input 
                    type="email"
                    value={form.email} 
                    onChange={(e) => setForm({...form, email: e.target.value})} 
                    placeholder="contact@example.com" 
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Physical Address (Optional)</label>
                <input 
                  value={form.address} 
                  onChange={(e) => setForm({...form, address: e.target.value})} 
                  placeholder="123 Main St, City" 
                />
              </div>

              <div className="form-group">
                <label>Contact Type</label>
                <select 
                  value={form.contactType} 
                  onChange={(e) => setForm({...form, contactType: e.target.value})}
                >
                  <option value="DEBTOR">Debtor (They owe me money)</option>
                  <option value="CREDITOR">Creditor (I owe them money)</option>
                  <option value="BOTH">Both (Mutual financial relationship)</option>
                </select>
              </div>

              <div className="form-group">
                <label>Notes</label>
                <textarea 
                  rows={2} 
                  value={form.notes} 
                  onChange={(e) => setForm({...form, notes: e.target.value})} 
                  placeholder="Any additional information..."
                />
              </div>

              <button type="submit" className="btn-primary" disabled={isSaving} style={{ marginTop: '0.5rem', justifyContent: 'center' }}>
                {isSaving ? 'Saving...' : (editingContact ? 'Save Changes' : 'Create Contact')}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Contacts;
