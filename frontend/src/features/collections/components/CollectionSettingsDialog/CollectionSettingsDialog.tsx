import React, { useState } from 'react';
import { Dialog, DialogTitle, DialogContent, Tabs, Tab, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import ModulesSection from './ModulesSection';
import MembersSection from './MembersSection';

export default function CollectionSettingsDialog({ open, onClose, collectionId, currentUserRole }:
  { open:boolean; onClose: ()=>void; collectionId:string; currentUserRole:string }){
  const [tab, setTab] = useState(0);
  if (!open) return null;

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>
        Collection Settings
        <IconButton onClick={onClose} sx={{ position:'absolute', right:8, top:8 }}><CloseIcon/></IconButton>
      </DialogTitle>
      <DialogContent>
        <Tabs value={tab} onChange={(_,v)=>setTab(v)}>
          <Tab label="Modules" />
          <Tab label="Members" />
        </Tabs>
        {tab === 0 && <ModulesSection collectionId={collectionId} />}
        {tab === 1 && <MembersSection collectionId={collectionId} currentUserRole={currentUserRole} />}
      </DialogContent>
    </Dialog>
  );
}
