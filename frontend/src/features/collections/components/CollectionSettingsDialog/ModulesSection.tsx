import React from 'react';
import { Box, List, ListItem, ListItemText, Switch, Typography } from '@mui/material';
import { useCollectionModules } from '../../hooks/useCollectionModules';

export default function ModulesSection({ collectionId }:{ collectionId:string }){
  const { modules, loading, toggle } = useCollectionModules(collectionId);

  return (
    <Box>
      <Typography variant="h6">Modules</Typography>
      <List>
        {modules.map(m => (
          <ListItem key={m.moduleKey} secondaryAction={<Switch checked={m.enabled} onChange={()=>toggle(m.moduleKey, !m.enabled)} />}>
            <ListItemText primary={m.moduleKey} secondary={m.description || ''} />
          </ListItem>
        ))}
      </List>
    </Box>
  );
}
